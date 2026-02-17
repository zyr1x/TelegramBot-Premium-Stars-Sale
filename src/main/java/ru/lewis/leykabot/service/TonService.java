package ru.lewis.leykabot.service;

import com.iwebpp.crypto.TweetNaclFast;
import org.springframework.stereotype.Service;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.mnemonic.Mnemonic;
import org.ton.ton4j.mnemonic.Pair;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.types.WalletV4R2Config;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.AddressInformationResponse;
import ru.lewis.leykabot.configuration.TonConfig;
import ru.lewis.leykabot.model.exception.TonRequestException;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;

@Service
public class TonService {
    private final TonConfig tonConfig;
    private final TonCenter client;

    private final ScheduledExecutorService scheduler;

    public TonService(TonConfig tonConfig) {
        this.tonConfig = tonConfig;
        this.client = createClient();
        this.scheduler = Executors.newScheduledThreadPool(10);
    }

    public CompletableFuture<SendResponse> send(String address, String payloadBase64, String amount) {
        var secret = getSecret();
        var wallet = createWallet(secret);
        wallet.setTonCenterClient(client);

        return checkConnectAsync(wallet).thenApply(seqno -> {
            try {
                Address toAddress = new Address(address);
                Cell body = Cell.fromBocBase64(payloadBase64);
                var amountBig = new BigInteger(amount).add(BigInteger.valueOf(1_000_000L));

                WalletV4R2Config config = WalletV4R2Config.builder()
                        .walletId(wallet.getWalletId())
                        .seqno(seqno)
                        .destination(toAddress)
                        .amount(amountBig)
                        .sendMode(SendMode.PAY_GAS_SEPARATELY)
                        .body(body)
                        .bounce(false)
                        .build();

                return wallet.send(config);
            } catch (Exception e) {
                throw new CompletionException(new TonRequestException("Error: transaction not sent: " + e));
            }
        });
    }

    private CompletableFuture<Long> checkConnectAsync(WalletV4R2 wallet) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        long timeout = tonConfig.getNetwork().getTimeout();
        long checkInterval = tonConfig.getNetwork().getCheckout();

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (future.isDone()) return;

            if (System.currentTimeMillis() - startTime >= timeout) {
                future.completeExceptionally(new TonRequestException("Timeout"));
                return;
            }

            try {
                long walletId = wallet.getWalletId();
                if (walletId == 0) {
                    System.out.println("Wallet not active, retrying...");
                    return;
                }

                long seqno = client.getSeqno(wallet.getAddress().toBounceable());
                future.complete(seqno); // бекнем seqno в future
            } catch (Exception e) {
                System.out.println("Wallet not ready, retrying...");
            }
        }, 0, checkInterval, TimeUnit.MILLISECONDS);

        // Отмена таска после завершения future
        future.whenComplete((r, ex) -> task.cancel(false));

        return future;
    }

    private WalletV4R2 createWallet(TweetNaclFast.Signature.KeyPair secret) {
        return WalletV4R2.builder()
                .keyPair(secret)
                .walletId(698983191)
                .build();
    }

    private TweetNaclFast.Signature.KeyPair getSecret() {
        try {
            String[] mnemonic = tonConfig.getWallet().getMnemonic().split(" ");
            Pair mnemonicPair = Mnemonic.toKeyPair(Arrays.asList(mnemonic));

            return TweetNaclFast.Signature.keyPair_fromSeed(mnemonicPair.getSecretKey());
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            exception.printStackTrace();
        }

        throw new TonRequestException("Secret not valid");
    }

    public String getBalance(String address) throws TonRequestException {
        TonResponse<AddressInformationResponse> response =
                client.getAddressInformation(address);

        if (response.isSuccess()) {
            AddressInformationResponse info = response.getResult();
            return info.getBalance();
        }

        throw new TonRequestException("Address not found");
    }

    public TonCenter createClient() {
        if (client != null) return client;
        return TonCenter.builder()
                .apiKey(tonConfig.getWallet().getApiKey())
                .mainnet()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }
}
