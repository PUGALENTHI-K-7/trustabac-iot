package com.trustabac.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Configuration
public class BlockchainConfig {

    private final BlockchainProperties blockchainProperties;

    public BlockchainConfig(BlockchainProperties blockchainProperties) {
        this.blockchainProperties = blockchainProperties;
    }

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(blockchainProperties.getRpcUrl()));
    }

    @Bean
    public Credentials credentials() {
        String privateKey = blockchainProperties.getPrivateKey();
        if (privateKey != null && !privateKey.isBlank()) {
            return Credentials.create(privateKey);
        }
        try {
            return Credentials.create(org.web3j.crypto.Keys.createEcKeyPair());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral blockchain credentials", e);
        }
    }

    @Bean
    public ContractGasProvider contractGasProvider() {
        return new StaticGasProvider(
                BigInteger.valueOf(blockchainProperties.getGasPrice()),
                BigInteger.valueOf(blockchainProperties.getGasLimit())
        );
    }
}
