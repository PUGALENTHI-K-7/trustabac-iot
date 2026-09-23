package com.trustabac.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trustabac.blockchain")
public class BlockchainProperties {

    private String rpcUrl = "http://127.0.0.1:8545";
    private Long chainId = 1337L;
    private String contractAddress;
    private String privateKey;
    private Long gasLimit = 3000000L;
    private Long gasPrice = 20000000000L; // 20 Gwei
    private boolean enabled = true;

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(Long gasLimit) {
        this.gasLimit = gasLimit;
    }

    public Long getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(Long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
