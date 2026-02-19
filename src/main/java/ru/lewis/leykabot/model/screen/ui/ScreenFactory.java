package ru.lewis.leykabot.model.screen.ui;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.PremiumConfig;
import ru.lewis.leykabot.configuration.StarsConfig;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.configuration.loc.KeyboardLocConfig;
import ru.lewis.leykabot.model.screen.ui.impl.*;
import ru.lewis.leykabot.model.screen.ui.impl.premium.BuyPremiumScreen;
import ru.lewis.leykabot.model.screen.ui.impl.premium.SelectUserForBuyPremiumScreen;
import ru.lewis.leykabot.model.screen.ui.impl.star.BuyStarsScreen;
import ru.lewis.leykabot.model.screen.ui.impl.star.SelectUserForBuyStarsScreen;
import ru.lewis.leykabot.service.*;

@Component
@AllArgsConstructor
public class ScreenFactory {

    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ScreenManager screenManager;
    private final TelegramService telegramService;
    private final TelegramConfig telegramConfig;
    private final KeyboardLocConfig keyboardLocConfig;
    private final TransactionService transactionService;
    private final UserService userService;
    private final ErrorMessageConfig errorMessageConfig;
    private final DevModeConfig devModeConfig;
    private final FragmentStarsService fragmentStarsService;
    private final StarsConfig starsConfig;
    private final TonService tonService;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;
    private final PremiumConfig premiumConfig;
    private final FragmentPremiumService fragmentPremiumService;

    public StartScreen createStartScreen(Long chatId, Long userId) {
        return new StartScreen(chatId, userId, clientMessageConfig, buttonsLocConfig, screenManager, telegramService, telegramConfig,this);
    }

    public ProfileScreen createProfileScreen(Long chatId, Long userId) {
        return new ProfileScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, screenManager, telegramService, userService,
                transactionService, starsTransactionService, premiumTransactionService, this);
    }

    public LinksScreen createLinksScreen(Long chatId, Long userId) {
        return new LinksScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, screenManager, telegramService,this);
    }

    public SupportScreen createSupportScreen(Long chatId, Long userId) {
        return new SupportScreen(chatId, userId, buttonsLocConfig, clientMessageConfig, screenManager, telegramService, this);
    }

    public DepositRublesScreen createDepositRublesScreen(Long chatId, Long userId) {
        return new DepositRublesScreen(chatId, userId,
                buttonsLocConfig, keyboardLocConfig, clientMessageConfig, transactionService,
                errorMessageConfig, telegramService, devModeConfig, screenManager, this);
    }

    public BuyStarsScreen createBuyStarsScreen(Long chatId, Long userId) {
        return new BuyStarsScreen(chatId, userId,
                buttonsLocConfig, keyboardLocConfig, clientMessageConfig, transactionService,
                errorMessageConfig, telegramService, devModeConfig, starsConfig, screenManager, this);
    }

    public SelectUserForBuyStarsScreen createSelectUserForBuyStarsScreen(Long chatId, Long userId, int stars, int rubles) {
        return new SelectUserForBuyStarsScreen(chatId, userId, stars, rubles,
                clientMessageConfig, buttonsLocConfig, telegramService, fragmentStarsService,
                errorMessageConfig, userService, tonService, starsTransactionService, screenManager, this);
    }

    public SubscribeChannelScreen createSubscribeChannelScreen(Long chatId, Long userId) {
        return new SubscribeChannelScreen(chatId, userId, telegramService, clientMessageConfig, telegramConfig, buttonsLocConfig, screenManager, this);
    }

    public ReferralScreen createReferralScreen(Long chatId, Long userId) {
        return new ReferralScreen(chatId, userId, userService, buttonsLocConfig, clientMessageConfig, telegramService, screenManager, this);
    }

    public BuyPremiumScreen createBuyPremiumScreen(Long chatId, Long userId) {
        return new BuyPremiumScreen(chatId, userId, buttonsLocConfig, keyboardLocConfig, clientMessageConfig, telegramService,
                devModeConfig, premiumConfig, screenManager, this);
    }

    public SelectUserForBuyPremiumScreen createSelectUserForBuyPremiumScreen(Long chatId, Long userId, int months, int rubles) {
        return new SelectUserForBuyPremiumScreen(chatId, userId, months, rubles, clientMessageConfig, buttonsLocConfig, errorMessageConfig, telegramService,
                fragmentPremiumService, premiumTransactionService, userService, tonService, screenManager, this);
    }
}