package ru.lewis.leykabot.service;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {

    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;

    /**
     * Создать новый промокод
     */
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

        telegramService.log(MessageFormat.format(logMessageConfig.getCreateCode(), code));

        return codeRepository.save(newCode);
    }

    /**
     * Создать промокод с автогенерацией
     */
    @Transactional
    public Code createRandomCode(Integer amount, Integer usageLimit, LocalDateTime expiresAt) {
        String randomCode = generateRandomCode();
        return createCode(randomCode, amount, usageLimit, expiresAt);
    }

    /**
     * Активировать промокод
     */
    @Transactional
    public ActivationResult activateCode(Long telegramId, String codeStr) {
        // Проверяем, что пользователь существует
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Проверяем, что код не использован этим пользователем
        if (activatedCodeRepository.existsByTelegramIdAndCode(telegramId, codeStr)) {
            log.warn("Пользователь {} уже активировал код {}", telegramId, codeStr);
            return new ActivationResult(false, "Вы уже использовали этот промокод", null);
        }

        // Находим код
        Code code = codeRepository.findByCode(codeStr).orElse(null);
        if (code == null) {
            log.warn("Промокод {} не найден", codeStr);
            return new ActivationResult(false, "Промокод не найден", null);
        }

        // Проверяем срок действия
        if (code.isExpired()) {
            log.warn("Промокод {} истёк", codeStr);
            return new ActivationResult(false, "Срок действия промокода истёк", null);
        }

        // Проверяем лимит использований
        if (!code.canBeUsed()) {
            log.warn("Промокод {} достиг лимита использований", codeStr);
            return new ActivationResult(false, "Промокод больше не действителен", null);
        }

        // Активируем
        ActivatedCode activated = new ActivatedCode();
        activated.setTelegramId(telegramId);
        activated.setCode(codeStr);
        activatedCodeRepository.save(activated);

        // Увеличиваем счетчик использований
        code.setUsedCount(code.getUsedCount() + 1);
        codeRepository.save(code);

        // Начисляем бонус
        user.setBalance(user.getBalance() + code.getAmount());
        userRepository.save(user);

        log.info("Пользователь {} активировал промокод {} и получил {} звёзд",
                telegramId, codeStr, code.getAmount());

        return new ActivationResult(true,
                "Промокод активирован! Вам начислено " + code.getAmount() + " звёзд",
                code.getAmount());
    }

    /**
     * Получить промокод по коду
     */
    public Optional<Code> getCodeByCode(String code) {
        return codeRepository.findByCode(code);
    }

    /**
     * Получить все активные промокоды
     */
    public List<Code> getAllActiveCodes() {
        return codeRepository.findAllActive(LocalDateTime.now());
    }

    /**
     * Получить все доступные промокоды (не истекшие и с лимитом)
     */
    public List<Code> getAllAvailableCodes() {
        return codeRepository.findAllAvailable(LocalDateTime.now());
    }

    /**
     * Получить историю активаций пользователя
     */
    public List<ActivatedCode> getUserActivatedCodes(Long telegramId) {
        return activatedCodeRepository.findByTelegramIdOrderByActivatedAtDesc(telegramId);
    }

    /**
     * Получить количество активаций промокода
     */
    public Long getCodeActivationCount(String code) {
        return activatedCodeRepository.countByCode(code);
    }

    /**
     * Удалить промокод
     */
    @Transactional
    public void deleteCode(String code) {
        Code codeEntity = codeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));

        codeRepository.delete(codeEntity);
        log.info("Промокод {} удалён", code);
    }

    /**
     * Очистить истёкшие промокоды
     */
    @Transactional
    public int cleanupExpiredCodes() {
        List<Code> expiredCodes = codeRepository.findAllExpired(LocalDateTime.now());
        codeRepository.deleteAll(expiredCodes);
        log.info("Удалено {} истёкших промокодов", expiredCodes.size());
        return expiredCodes.size();
    }

    /**
     * Генерация случайного кода
     */
    private String generateRandomCode() {
        return "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Результат активации промокода
     */
    public record ActivationResult(boolean success, String message, Integer bonusAmount) {}
}
