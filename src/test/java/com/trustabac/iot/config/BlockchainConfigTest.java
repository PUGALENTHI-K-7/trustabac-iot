package com.trustabac.iot.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BlockchainConfigTest {

    @Autowired
    private BlockchainProperties blockchainProperties;

    @Autowired
    private Web3j web3j;

    @Autowired
    private Credentials credentials;

    @Autowired
    private ContractGasProvider contractGasProvider;

    @Test
    @DisplayName("BlockchainProperties and Web3j beans should be properly injected and configured")
    void shouldLoadBlockchainBeans() {
        assertThat(blockchainProperties).isNotNull();
        assertThat(blockchainProperties.getRpcUrl()).isEqualTo("http://127.0.0.1:8545");
        assertThat(blockchainProperties.getChainId()).isEqualTo(1337L);
        assertThat(blockchainProperties.isEnabled()).isTrue();

        assertThat(web3j).isNotNull();
        assertThat(credentials).isNotNull();
        assertThat(credentials.getAddress()).isNotBlank();
        assertThat(contractGasProvider).isNotNull();
    }
}
