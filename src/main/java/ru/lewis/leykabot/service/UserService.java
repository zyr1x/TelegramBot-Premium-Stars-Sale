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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final ActivatedReferralRepository activatedReferralRepository;
    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;
    private final TelegramBotConfig telegramBotConfig;

    // Кэш: telegramId -> User
    private Cache<Long, User> userCache;
    // Кэш: userId -> список реферальных ссылок пользователя
    private Cache<Long, List<Referral>> referralCache;
    // Кэш: hash -> Referral
    private Cache<String, Referral> referralByHashCache;
    // Кэш: telegramId -> список активированных рефералов (кем активированы)
    private Cache<Long, List<ActivatedReferral>> activatedReferralCache;

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
    }

    // -------------------------------------------------------------------------
    // Прогрев / инвалидация кэша
    // -------------------------------------------------------------------------

    /** Подгружает пользователя в кэш. Если уже есть — возвращает из кэша. */
    public Optional<User> warmUp(Long telegramId) {
        return Optional.ofNullable(
                userCache.get(telegramId,
                        id -> userRepository.findByTelegramId(id).orElse(null))
        );
    }

    /** Подгружает реферальные ссылки пользователя в кэш. */
    public List<Referral> warmUpReferrals(Long userId) {
        return referralCache.get(userId, id -> referralRepository.findAllByUserId(id));
    }

    /** Подгружает активированные рефералы пользователя в кэш. */
    public List<ActivatedReferral> warmUpActivatedReferrals(Long userId) {
        return activatedReferralCache.get(userId,
                id -> activatedReferralRepository.findByActivatedByUserId(id));
    }

    /** Принудительно обновляет кэш пользователя из БД. */
    public Optional<User> refreshCache(Long telegramId) {
        Optional<User> fresh = userRepository.findByTelegramId(telegramId);
        if (fresh.isPresent()) {
            userCache.put(telegramId, fresh.get());
        } else {
            userCache.invalidate(telegramId);
        }
        return fresh;
    }

    public void invalidateCache(Long telegramId) {
        userCache.invalidate(telegramId);
    }

    public void invalidateReferralCache(Long userId) {
        referralCache.invalidate(userId);
    }

    public void invalidateActivatedReferralCache(Long userId) {
        activatedReferralCache.invalidate(userId);
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

    /** Из кэша — если есть запись, пользователь существует. */
    public boolean isUserExists(Long telegramId) {
        return warmUp(telegramId).isPresent();
    }

    /** Из кэша. */
    public Optional<Integer> getBalance(Long telegramId) {
        return warmUp(telegramId).map(User::getBalance);
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

        // кладём в оба кэша сразу
        referralByHashCache.put(hash, saved);
        referralCache.invalidate(userId); // список изменился — инвалидируем

        return hashToLink(hash);
    }

    @Transactional
    public boolean activateReferral(String hash, Long newUserId) {
        // Проверяем через кэш активаций
        boolean alreadyActivated = warmUpActivatedReferrals(newUserId).stream()
                .anyMatch(a -> a.getReferralHash().equals(hash));
        if (alreadyActivated) {
            return false;
        }

        // Находим реферал через кэш
        Referral referral = referralByHashCache.get(hash,
                h -> referralRepository.findByHash(h).orElse(null));
        if (referral == null || referral.getUserId().equals(newUserId)) {
            return false;
        }

        // Сохраняем активацию
        ActivatedReferral activated = new ActivatedReferral();
        activated.setReferralHash(hash);
        activated.setActivatedByUserId(newUserId);
        activatedReferralRepository.save(activated);

        // Инвалидируем кэш активаций нового пользователя
        activatedReferralCache.invalidate(newUserId);

        // Начисляем бонус реферреру — обновляем и в БД, и в кэше
        User referrer = userCache.get(referral.getUserId(),
                id -> userRepository.findByTelegramId(id).orElse(null));
        if (referrer != null) {
            userRepository.save(referrer);
            userCache.put(referrer.getTelegramId(), referrer);
        }

        return true;
    }

    public Optional<Long> getReferralOwner(String hash) {
        Referral referral = referralByHashCache.get(hash,
                h -> referralRepository.findByHash(h).orElse(null));
        return Optional.ofNullable(referral).map(Referral::getUserId);
    }

    public boolean isReferralHashExists(String hash) {
        Referral referral = referralByHashCache.get(hash,
                h -> referralRepository.findByHash(h).orElse(null));
        return referral != null;
    }

    /** Список реферальных ссылок пользователя — из кэша. */
    public List<Referral> getUserReferrals(Long userId) {
        return warmUpReferrals(userId);
    }

    public boolean hasUserReferrals(Long userId) {
        return !warmUpReferrals(userId).isEmpty();
    }

    /** Количество активированных рефералов пользователя — из кэша. */
    public long getReferralActivationCount(Long userId) {
        return warmUpActivatedReferrals(userId).size();
    }

    public boolean hasReferralActivation(Long userId) {
        return !warmUpActivatedReferrals(userId).isEmpty();
    }

    public String hashToLink(String hash) {
        return "https://t.me/" + telegramBotConfig.getName() + "?start=" + hash;
    }

    // -------------------------------------------------------------------------
    // Промокоды
    // -------------------------------------------------------------------------

    @Transactional
    public boolean activateCode(Long telegramId, String codeStr) {
        if (activatedCodeRepository.existsByTelegramIdAndCode(telegramId, codeStr)) {
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

        // Обновляем баланс в БД и сразу в кэше
        User user = userCache.get(telegramId,
                id -> userRepository.findByTelegramId(id).orElse(null));
        if (user != null) {
            user.setBalance(user.getBalance() + code.getAmount());
            userRepository.save(user);
            userCache.put(telegramId, user);
        }

        return true;
    }
}