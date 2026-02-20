package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.model.database.entity.ActivatedCode;
import ru.lewis.leykabot.model.database.entity.Code;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.ActivatedCodeRepository;
import ru.lewis.leykabot.repository.CodeRepository;
import ru.lewis.leykabot.repository.UserRepository;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {

    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;

    // Кэш: codeStr -> Code (без Optional — Caffeine не хранит null)
    private Cache<String, Code> codeCache;
    // Кэш: telegramId -> список активированных кодов пользователя
    private Cache<Long, List<ActivatedCode>> userActivatedCodesCache;

    @PostConstruct
    public void initCaches() {
        codeCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        userActivatedCodesCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    // -------------------------------------------------------------------------
    // Прогрев / инвалидация
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> warmUpAll(Long telegramId) {
        return CompletableFuture.runAsync(() -> warmUpUserCodes(telegramId));
    }

    /** Подгружает активированные коды пользователя в кэш. Если уже есть — из кэша. */
    public List<ActivatedCode> warmUpUserCodes(Long telegramId) {
        return userActivatedCodesCache.get(telegramId,
                id -> activatedCodeRepository.findByTelegramIdOrderByActivatedAtDesc(id));
    }

    /** Подгружает все промокоды из БД в кэш за один запрос. */
    public CompletableFuture<Void> warmUpAllCodes() {
        return CompletableFuture.runAsync(() -> codeRepository.findAll().forEach(code -> codeCache.put(code.getCode(), code)));
    }

    /** Подгружает конкретный промокод в кэш. Если уже есть — из кэша. */
    public Optional<Code> warmUpCode(String codeStr) {
        return Optional.ofNullable(
                codeCache.get(codeStr,
                        key -> codeRepository.findByCode(key).orElse(null))
        );
    }

    /** Принудительно обновляет кэш активированных кодов пользователя из БД. */
    public List<ActivatedCode> refreshUserCodesCache(Long telegramId) {
        List<ActivatedCode> fresh =
                activatedCodeRepository.findByTelegramIdOrderByActivatedAtDesc(telegramId);
        userActivatedCodesCache.put(telegramId, fresh);
        return fresh;
    }

    /** Принудительно обновляет кэш промокода из БД. */
    public Optional<Code> refreshCodeCache(String codeStr) {
        Optional<Code> fresh = codeRepository.findByCode(codeStr);
        if (fresh.isPresent()) {
            codeCache.put(codeStr, fresh.get());
        } else {
            codeCache.invalidate(codeStr);
        }
        return fresh;
    }

    public void invalidateUserCodesCache(Long telegramId) {
        userActivatedCodesCache.invalidate(telegramId);
    }

    public void invalidateCodeCache(String codeStr) {
        codeCache.invalidate(codeStr);
    }

    // -------------------------------------------------------------------------
    // Основные методы — все через кэш
    // -------------------------------------------------------------------------

    @Transactional
    public Code createCode(String code, Integer amount, Integer usageLimit, LocalDateTime expiresAt) {
        if (codeRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Код уже существует");
        }
        Code newCode = new Code();
        newCode.setCode(code);
        newCode.setAmount(amount);
        newCode.setUsageLimit(usageLimit);
        newCode.setUsedCount(0);
        newCode.setExpiresAt(expiresAt);
        Code saved = codeRepository.save(newCode);
        codeCache.put(code, saved);
        telegramService.log(MessageFormat.format(logMessageConfig.getCreateCode(), code));
        return saved;
    }

    @Transactional
    public Code createRandomCode(Integer amount, Integer usageLimit, LocalDateTime expiresAt) {
        String randomCode = "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return createCode(randomCode, amount, usageLimit, expiresAt);
    }

    @Transactional
    public ActivationResult activateCode(Long telegramId, String codeStr) {
        // Пользователь — из кэша UserService, здесь тянем напрямую только если нет зависимости
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Проверяем дубль — через кэш активаций
        boolean alreadyUsed = warmUpUserCodes(telegramId).stream()
                .anyMatch(a -> a.getCode().equals(codeStr));
        if (alreadyUsed) {
            log.warn("Пользователь {} уже активировал код {}", telegramId, codeStr);
            return new ActivationResult(false, "Вы уже использовали этот промокод", null);
        }

        // Промокод — через кэш
        Code code = codeCache.get(codeStr,
                key -> codeRepository.findByCode(key).orElse(null));
        if (code == null) {
            log.warn("Промокод {} не найден", codeStr);
            return new ActivationResult(false, "Промокод не найден", null);
        }
        if (code.isExpired()) {
            log.warn("Промокод {} истёк", codeStr);
            return new ActivationResult(false, "Срок действия промокода истёк", null);
        }
        if (!code.canBeUsed()) {
            log.warn("Промокод {} достиг лимита использований", codeStr);
            return new ActivationResult(false, "Промокод больше не действителен", null);
        }

        // Сохраняем активацию
        ActivatedCode activated = new ActivatedCode();
        activated.setTelegramId(telegramId);
        activated.setCode(codeStr);
        activatedCodeRepository.save(activated);

        // Обновляем счётчик и кэш промокода
        code.setUsedCount(code.getUsedCount() + 1);
        codeRepository.save(code);
        codeCache.put(codeStr, code);

        // Обновляем баланс
        user.setBalance(user.getBalance() + code.getAmount());
        userRepository.save(user);

        // Инвалидируем список активаций — он изменился
        userActivatedCodesCache.invalidate(telegramId);

        log.info("Пользователь {} активировал промокод {} и получил {} звёзд",
                telegramId, codeStr, code.getAmount());

        return new ActivationResult(true,
                "Промокод активирован! Вам начислено " + code.getAmount() + " звёзд",
                code.getAmount());
    }

    /** Промокод по строке — из кэша. */
    public Optional<Code> getCodeByCode(String codeStr) {
        return Optional.ofNullable(
                codeCache.get(codeStr,
                        key -> codeRepository.findByCode(key).orElse(null))
        );
    }

    /** Количество активаций промокода — из кэша активаций не считаем глобально,
     *  поэтому точечный запрос оправдан (это не per-user данные). */
    public Long getCodeActivationCount(String codeStr) {
        return activatedCodeRepository.countByCode(codeStr);
    }

    /** Активированные коды пользователя — из кэша. */
    public List<ActivatedCode> getUserActivatedCodes(Long telegramId) {
        return warmUpUserCodes(telegramId);
    }

    /** Количество использованных промокодов пользователем — из кэша. */
    public long getUserActivatedCodesCount(Long telegramId) {
        return warmUpUserCodes(telegramId).size();
    }

    /** Активные промокоды — не кэшируем, список глобальный и часто меняется. */
    public List<Code> getAllActiveCodes() {
        return codeRepository.findAllActive(LocalDateTime.now());
    }

    /** Доступные промокоды — не кэшируем по той же причине. */
    public List<Code> getAllAvailableCodes() {
        return codeRepository.findAllAvailable(LocalDateTime.now());
    }

    @Transactional
    public void deleteCode(String codeStr) {
        Code entity = codeCache.get(codeStr,
                key -> codeRepository.findByCode(key).orElse(null));
        if (entity == null) {
            throw new IllegalArgumentException("Промокод не найден");
        }
        codeRepository.delete(entity);
        codeCache.invalidate(codeStr);
        log.info("Промокод {} удалён", codeStr);
    }

    @Transactional
    public int cleanupExpiredCodes() {
        List<Code> expired = codeRepository.findAllExpired(LocalDateTime.now());
        expired.forEach(c -> codeCache.invalidate(c.getCode()));
        codeRepository.deleteAll(expired);
        log.info("Удалено {} истёкших промокодов", expired.size());
        return expired.size();
    }

    public record ActivationResult(boolean success, String message, Integer bonusAmount) {}
}