package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.TelegramBotConfig;
import ru.lewis.leykabot.model.database.entity.*;
import ru.lewis.leykabot.repository.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final ActivatedReferralRepository activatedReferralRepository;
    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;
    private final TelegramBotConfig telegramBotConfig;

    // telegramId -> User
    private Cache<Long, User> userCache;
    // userId -> список реферальных ссылок
    private Cache<Long, List<Referral>> referralCache;
    // hash -> Referral
    private Cache<String, Referral> referralByHashCache;
    // userId -> список активированных рефералов
    private Cache<Long, List<ActivatedReferral>> activatedReferralCache;
    // telegramId+code -> Boolean (активирован ли промокод)
    private Cache<String, Boolean> activatedCodeCache;

    @PostConstruct
    public void initCaches() {
        userCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        referralCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        referralByHashCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        activatedReferralCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        activatedCodeCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы загрузки в кэш
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> warmUpAll(Long telegramId) {
        return CompletableFuture.runAsync(() -> {
            User user = loadUser(telegramId);
            if (user == null) return;

            Long userId = user.getTelegramId();

            loadReferrals(userId);
            loadActivatedReferrals(userId);
        });
    }

    private User loadUser(Long telegramId) {
        return userCache.get(telegramId,
                id -> userRepository.findByTelegramId(id).orElse(null));
    }

    private List<Referral> loadReferrals(Long userId) {
        return referralCache.get(userId,
                id -> referralRepository.findAllByUserId(id));
    }

    private List<ActivatedReferral> loadActivatedReferrals(Long userId) {
        return activatedReferralCache.get(userId,
                id -> activatedReferralRepository.findByActivatedByUserId(id));
    }

    private Referral loadReferralByHash(String hash) {
        return referralByHashCache.get(hash,
                h -> referralRepository.findByHash(h).orElse(null));
    }

    private boolean loadActivatedCode(Long telegramId, String code) {
        return activatedCodeCache.get(telegramId + ":" + code,
                key -> activatedCodeRepository.existsByTelegramIdAndCode(telegramId, code));
    }

    // -------------------------------------------------------------------------
    // Пользователь
    // -------------------------------------------------------------------------

    @Transactional
    public User createUser(Long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setBalance(0);
        User saved = userRepository.save(user);
        userCache.put(telegramId, saved);
        return saved;
    }

    public boolean isUserExists(Long telegramId) {
        return loadUser(telegramId) != null;
    }

    public Optional<User> getUser(Long telegramId) {
        return Optional.ofNullable(loadUser(telegramId));
    }

    public Optional<Integer> getBalance(Long telegramId) {
        return getUser(telegramId).map(User::getBalance);
    }

    public Optional<User> refreshUserCache(Long telegramId) {
        Optional<User> fresh = userRepository.findByTelegramId(telegramId);
        fresh.ifPresentOrElse(
                u -> userCache.put(telegramId, u),
                () -> userCache.invalidate(telegramId)
        );
        return fresh;
    }

    public void invalidateUserCache(Long telegramId) {
        userCache.invalidate(telegramId);
    }

    // -------------------------------------------------------------------------
    // Рефералы
    // -------------------------------------------------------------------------

    @Transactional
    public String createReferralLink(Long userId) {
        String hash = UUID.randomUUID().toString().substring(0, 8);

        Referral referral = new Referral();
        referral.setUserId(userId);
        referral.setHash(hash);
        Referral saved = referralRepository.save(referral);

        // Кладём в кэш по хэшу
        referralByHashCache.put(hash, saved);

        // Добавляем в существующий список, если он уже закэшен — не инвалидируем
        List<Referral> cached = referralCache.getIfPresent(userId);
        if (cached != null) {
            cached.add(saved);
        } else {
            referralCache.put(userId, new ArrayList<>(List.of(saved)));
        }

        return hashToLink(hash);
    }

    @Transactional
    public boolean activateReferral(String hash, Long newUserId) {
        // Проверяем через кэш — уже активирован?
        boolean alreadyActivated = loadActivatedReferrals(newUserId).stream()
                .anyMatch(a -> a.getReferralHash().equals(hash));
        if (alreadyActivated) {
            return false;
        }

        // Ищем реферал через кэш
        Referral referral = loadReferralByHash(hash);
        if (referral == null || referral.getUserId().equals(newUserId)) {
            return false;
        }

        // Сохраняем активацию
        ActivatedReferral activated = new ActivatedReferral();
        activated.setReferralHash(hash);
        activated.setActivatedByUserId(newUserId);
        activatedReferralRepository.save(activated);

        // Обновляем кэш активаций — добавляем запись, не идём в БД
        List<ActivatedReferral> cachedActivations = activatedReferralCache.getIfPresent(newUserId);
        if (cachedActivations != null) {
            cachedActivations.add(activated);
        } else {
            activatedReferralCache.put(newUserId, new ArrayList<>(List.of(activated)));
        }

        // Начисляем бонус реферреру — обновляем и в БД, и в кэше
        User referrer = loadUser(referral.getUserId());
        if (referrer != null) {
            userRepository.save(referrer);
            userCache.put(referrer.getTelegramId(), referrer);
        }

        return true;
    }

    public Optional<Long> getReferralOwner(String hash) {
        return Optional.ofNullable(loadReferralByHash(hash)).map(Referral::getUserId);
    }

    public boolean isReferralHashExists(String hash) {
        return loadReferralByHash(hash) != null;
    }

    public List<Referral> getUserReferrals(Long userId) {
        return loadReferrals(userId);
    }

    public boolean hasUserReferrals(Long userId) {
        return !loadReferrals(userId).isEmpty();
    }

    public long getReferralActivationCount(Long userId) {
        return loadActivatedReferrals(userId).size();
    }

    public boolean hasReferralActivation(Long userId) {
        return !loadActivatedReferrals(userId).isEmpty();
    }

    public void invalidateReferralCache(Long userId) {
        referralCache.invalidate(userId);
    }

    public void invalidateActivatedReferralCache(Long userId) {
        activatedReferralCache.invalidate(userId);
    }

    // -------------------------------------------------------------------------
    // Промокоды
    // -------------------------------------------------------------------------

    @Transactional
    public boolean activateCode(Long telegramId, String codeStr) {
        // Проверяем через кэш — уже активирован?
        if (loadActivatedCode(telegramId, codeStr)) {
            return false;
        }

        Code code = codeRepository.findByCode(codeStr).orElse(null);
        if (code == null || !code.canBeUsed()) {
            return false;
        }

        ActivatedCode activated = new ActivatedCode();
        activated.setTelegramId(telegramId);
        activated.setCode(codeStr);
        activatedCodeRepository.save(activated);

        codeRepository.incrementUsedCount(codeStr);

        // Кэшируем факт активации промокода
        activatedCodeCache.put(telegramId + ":" + codeStr, Boolean.TRUE);

        // Обновляем баланс в БД и в кэше
        User user = loadUser(telegramId);
        if (user != null) {
            user.setBalance(user.getBalance() + code.getAmount());
            userRepository.save(user);
            userCache.put(telegramId, user);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Утилиты
    // -------------------------------------------------------------------------

    public String hashToLink(String hash) {
        return "https://t.me/" + telegramBotConfig.getName() + "?start=" + hash;
    }
}