package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.TelegramBotConfig;
import ru.lewis.leykabot.model.database.entity.*;
import ru.lewis.leykabot.repository.*;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ReferralRepository referralRepository;
    private final ActivatedReferralRepository activatedReferralRepository;
    private final CodeRepository codeRepository;
    private final ActivatedCodeRepository activatedCodeRepository;
    private final TelegramBotConfig telegramBotConfig;

    @Transactional
    public User createUser(Long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setBalance(0);
        return userRepository.save(user);
    }

    @Transactional
    public boolean isUserExists(Long telegramId) {
        return userRepository.existsByTelegramId(telegramId);
    }

    @Transactional
    public Optional<Integer> getBalance(Long telegramId) {
        return userRepository.getBalanceByTelegramId(telegramId);
    }

    @Transactional
    public String createReferralLink(Long userId) {
        String hash = UUID.randomUUID().toString().substring(0, 8);

        Referral referral = new Referral();
        referral.setUserId(userId);
        referral.setHash(hash);

        referralRepository.save(referral);

        return "https://t.me/" + telegramBotConfig.getName() + "?start=" + hash;
    }

    @Transactional
    public boolean activateReferral(String hash, Long newUserId) {
        // Проверяем, что не активировал уже
        if (activatedReferralRepository.existsByReferralHashAndActivatedByUserId(hash, newUserId)) {
            return false;
        }

        // Находим реферальную ссылку
        Referral referral = referralRepository.findByHash(hash).orElse(null);
        if (referral == null || referral.getUserId().equals(newUserId)) {
            return false; // Нельзя активировать свою ссылку
        }

        // Активируем
        ActivatedReferral activated = new ActivatedReferral();
        activated.setReferralHash(hash);
        activated.setActivatedByUserId(newUserId);
        activatedReferralRepository.save(activated);

        // Начисляем бонус реферреру
        User referrer = userRepository.findByTelegramId(referral.getUserId()).orElse(null);
        if (referrer != null) {
            referrer.setBalance(referrer.getBalance() + 100); // Бонус за реферала
            userRepository.save(referrer);
        }

        return true;
    }

    @Transactional
    public boolean activateCode(Long telegramId, String codeStr) {
        // Проверяем, что код не использован этим пользователем
        if (activatedCodeRepository.existsByTelegramIdAndCode(telegramId, codeStr)) {
            return false;
        }

        // Находим код
        Code code = codeRepository.findByCode(codeStr).orElse(null);
        if (code == null || !code.canBeUsed()) {
            return false;
        }

        // Активируем
        ActivatedCode activated = new ActivatedCode();
        activated.setTelegramId(telegramId);
        activated.setCode(codeStr);
        activatedCodeRepository.save(activated);

        // Увеличиваем счетчик использований
        codeRepository.incrementUsedCount(codeStr);

        // Начисляем бонус
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user != null) {
            user.setBalance(user.getBalance() + code.getAmount());
            userRepository.save(user);
        }

        return true;
    }
}
