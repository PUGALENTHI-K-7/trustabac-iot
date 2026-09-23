package com.trustabac.iot.blockchain.contract;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Web3j Type-Safe Java Wrapper for AdaptiveAccessControl.sol
 */
@SuppressWarnings("rawtypes")
public class AdaptiveAccessControl extends Contract {

    public static final String BINARY = "608060405234801562000010575f80fd5b5060405162001150380380620011508339810160408190526200003391620000a6565b335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060465f60146101000a81548160ff021916908360ff160217905550601e5f60156101000a81548160ff021916908360ff160217905550601e5f60166101000a81548160ff021916908360ff16021790555060465f60176101000a81548160ff021916908360ff1602179055507f1be30076a0d4c82b4dc8074d284f1837ce51f49638c4c34a21199a099c262d026046601e601e604660405160200162000102949392919062000216565b604051602081830303815290604052a1620002b8565b5f60208284031215620000b7575f80fd5b5f620000c484828501620001ff565b91505092915050565b5f60ff82169050919050565b5f819050919050565b5f620000f082620000ca565b9050919050565b620000fd81620000e6565b82525050565b5f608082019050620001155f830188620000f4565b620001216020830187620000f4565b6200012d6040830186620000f4565b620001396060830185620000f4565b9695505050505050565b5f81519050919050565b5f82825260208201905092915050565b5f5b838110156200017a5780820151818401526020810190506200015f565b5f84840152505055565b5f602082019050919050565b5f602082019050919050565b5f81905092915050565b5f620001b282620000ca565b9050919050565b620001bf81620001a8565b82525050565b5f604082019050620001d35f830185620001b6565b620001df6020830184620001b6565b9392505050565b5f60ff82169050919050565b5f819050919050565b5f6200020d82620001e7565b9050919050565b6200021a8162000203565b82525050565b5f608082019050620002325f83018862000211565b6200023e602083018762000211565b6200024a604083018662000211565b62000256606083018562000211565b9695505050505050565b620002c7565b620002b582620000ca565b9050919050565b620002c481620002ab565b82525050565b5f602082019050620002d95f830184620002bb565b92915050565b5f8154905090565b5f819050919050565b5f620002fb82620002df565b9050919050565b6200030881620002f1565b82525050565b5f6020820190506200031d5f830184620002ff565b92915050565b610e2080620003325f395ff3fe6080604052348015610010575f80fd5b5060043610610098575f3560e01c806307fa979f1461009c5780635da24c0d146100b75780638541c88e146100e45780638da5cb5b146101115780639912ce621461012e578063a8a3ce251461014b578063dfc8270514610178578063f2fde38b14610195578063fc7d4fc4146101ae575b5f80fd5b6100a56101c7565b6040516100ae9190610996565b60405180910390f35b6100ce6100c9366004610a26565b6101ce565b6040516100db929190610a5c565b60405180910390f35b6100fb6100f6366004610a76565b610360565b604051610108929190610a5c565b60405180910390f35b6101196104bc565b6040516101259190610ac1565b60405180910390f35b6101376104e1565b6040516101429190610996565b60405180910390f35b61016261015d366004610ae3565b6104f6565b60405161016f9190610996565b60405180910390f35b610180610582565b60405161018c9190610996565b60405180910390f35b61019d610597565b6040516101a59190610996565b60405180910390f35b6101b76105ac565b6040516101bf9190610996565b60405180910390f35b5f546014900460ff1681565b5f8083838586888a6101dd565b915091508989838387888a8c426040516101fa989796959493929190610b64565b60405180910390a3818193509350505098975050505050505050565b5f5473ffffffffffffffffffffffffffffffffffffffff1633146102145760405162461bcd60e51b815260040161020b90610c12565b60405180910390fd5b8082116102555760405162461bcd60e51b815260040161024c90610c4d565b60405180910390fd5b815f60166101000a81548160ff021916908360ff160217905550805f60176101000a81548160ff021916908360ff1602179055507f1be30076a0d4c82b4dc8074d284f1837ce51f49638c4c34a21199a099c262d025f546014900460ff165f546015900460ff1684846040516020016102ca9493929190610c88565b604051602081830303815290604052a15050565b5f5473ffffffffffffffffffffffffffffffffffffffff16331461031c5760405162461bcd60e51b815260040161031390610c12565b60405180910390fd5b80821161035d5760405162461bcd60e51b815260040161035490610d02565b60405180910390fd5b815f60146101000a81548160ff021916908360ff160217905550805f60156101000a81548160ff021916908360ff1602179055507f1be30076a0d4c82b4dc8074d284f1837ce51f49638c4c34a21199a099c262d0282825f546016900460ff165f546017900460ff166040516020016103d29493929190610c88565b604051602081830303815290604052a15050565b5f80866103f6575f9150600190506104ab565b85610406575f9150600290506104ab565b815f546015900460ff161015610423575f9150600390506104ab565b805f546017900460ff161115610440575f9150600490506104ab565b815f546014900460ff16101580156104655750805f546016900460ff1611155b1561047557600291505f90506104ab565b60019150600590505b9695505050505050565b5f5473ffffffffffffffffffffffffffffffffffffffff1690565b5f546016900460ff1681565b5f5473ffffffffffffffffffffffffffffffffffffffff1633146105345760405162461bcd60e51b815260040161052b90610c12565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156105775760405162461bcd60e51b815260040161056e90610d3d565b60405180910390fd5b7f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e05f5473ffffffffffffffffffffffffffffffffffffffff16826040516105be9190610ac1565b60405180910390a3805f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b5f546015900460ff1681565b5f546017900460ff1681565b60ff811681146105be575f80fd5b5f81519050919050565b5f82825260208201905092915050565b5f5b838110156105fd5780820151818401526020810190506105e2565b5f84840152505055565b5f602082019050919050565b5f602082019050919050565b5f81905092915050565b5f610634826105c3565b9050919050565b61064481610629565b82525050565b5f60208201905061065c5f83018461063b565b92915050565b5f60ff82169050919050565b5f819050919050565b5f61067d82610662565b9050919050565b61068d81610672565b82525050565b5f6020820190506106a55f830184610684565b92915050565b5f60ff82169050919050565b5f819050919050565b5f6106c6826106ab565b9050919050565b6106d6816106bb565b82525050565b5f6020820190506106ee5f8301846106cd565b92915050565b5f60ff82169050919050565b5f819050919050565b5f61070f826106f4565b9050919050565b61071f81610704565b82525050565b5f6020820190506107375f830184610716565b92915050565b5f6020828403121561074a575f80fd5b5f610757848285016105c3565b91505092915050565b5f60408284031215610772565f80fd5b5f61077f848285016105c3565b91506020610790848285016105c3565b90509250929050565b5f604082840312156107a9565f80fd5b5f6107b6848285016105c3565b915060206107c7848285016105c3565b90509250929050565b5f60ff82169050919050565b5f819050919050565b5f6107e8826107cd565b9050919050565b6107f8816107dd565b82525050565b5f6020820190506108105f8301846107ef565b92915050565b5f60208284031215610823575f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff848285013516905091505092915050565b5f8115159050919050565b5f60c08284031215610866575f80fd5b5f6108738482850161083f565b915060206108848482850161083f565b90506040610895848285016105c3565b60606108a6858286016105c3565b60806108b7868287016105c3565b60a06108c8878288016105c3565b94509450945094509450949050565b5f813590506108e4816107cd565b919050565b5f61010082840312156108f9575f80fd5b5f83013591506020830135905060406109158482850161083f565b60606109268582860161083f565b6080610937868287016108d6565b60a0610948878288016108d6565b60c0610959888289016108d6565b60e061096a89828a016108d6565b965096509650965096509650969050565b5f60ff82169050919050565b5f819050919050565b5f61099b82610980565b9050919050565b6109ab81610990565b82525050565b5f6020820190506109c35f8301846109a2565b92915050565b5f60ff82169050919050565b5f819050919050565b5f6109e4826109c9565b9050919050565b6109f4816109d9565b82525050565b5f602082019050610a0c5f8301846109eb565b92915050565b5f60ff82169050919050565b5f819050919050565b5f610a2d82610a12565b9050919050565b610a3d81610a22565b82525050565b5f602082019050610a555f830184610a34565b92915050565b5f604082019050610a715f830185610a01565b610a7f6020830184610a4a565b9392505050565b5f60c08284031215610a8d575f80fd5b5f610a9a8482850161083f565b91506020610aab8482850161083f565b90506040610abc848285016105c3565b6060610acd858286016105c3565b6080610ade868287016105c3565b60a0610aef878288016105c3565b94509450945094509450949050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610b2582610af6565b9050919050565b610b3581610b1a565b82525050565b5f602082019050610b4d5f830184610b2c565b92915050565b5f60408284031215610b66575f80fd5b5f610b73848285016105c3565b91506020610b84848285016105c3565b90509250929050565b5f60c082019050610ba15f830188610a01565b610baf6020830187610a4a565b610bbd6040830186610a4a565b610bcb6060830185610a4a565b610bd96080830184610a4a565b610be760a0830183610bf9565b9695505050505050565b5f819050919050565b5f610c0a82610bef565b9050919050565b610c1a81610bff565b82525050565b5f602082019050610c325f830184610c11565b92915050565b5f602082019050818152602001602760f81b8152602001602a8060281b60145190525060400191505060405160208183030381529060405290565b5f602082019050818152602001604160f81b815260200160438060281b60145190525060400191505060405160208183030381529060405290565b5f608082019050610ca55f830188610a4a565b610cb36020830187610a4a565b610cc16040830186610a4a565b610ccf6060830185610a4a565b9695505050505050565b5f602082019050818152602001604060f81b815260200160428060281b60145190525060400191505060405160208183030381529060405290565b5f602082019050818152602001602f60f81b815260200160328060281b6014519052506040019150506040516020818303038152906040529056fea2646970667358221220a273bebebb944883525287eb178b5490bc8e4420e7d6eeae471ffbc98e945c7d64736f6c63430008140033";

    public static final String FUNC_CALCULATEDECISION = "calculateDecision";
    public static final String FUNC_EVALUATEACCESS = "evaluateAccess";
    public static final String FUNC_OWNER = "owner";
    public static final String FUNC_RISKLOW = "riskLow";
    public static final String FUNC_RISKMEDIUM = "riskMedium";
    public static final String FUNC_SETRISKTHRESHOLDS = "setRiskThresholds";
    public static final String FUNC_SETTRUSTTHRESHOLDS = "setTrustThresholds";
    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";
    public static final String FUNC_TRUSTHIGH = "trustHigh";
    public static final String FUNC_TRUSTMEDIUM = "trustMedium";

    public static final Event AUTHORIZATIONEVALUATED_EVENT = new Event("AuthorizationEvaluated",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Bytes32>(true) {},
                    new TypeReference<Bytes32>(true) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint256>(false) {}
            ));

    public static final Event THRESHOLDSUPDATED_EVENT = new Event("ThresholdsUpdated",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {},
                    new TypeReference<Uint8>(false) {}
            ));

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},
                    new TypeReference<Address>(true) {}
            ));

    protected AdaptiveAccessControl(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, credentials, gasProvider);
    }

    protected AdaptiveAccessControl(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, gasProvider);
    }

    public static RemoteCall<AdaptiveAccessControl> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AdaptiveAccessControl.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<AdaptiveAccessControl> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AdaptiveAccessControl.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    public static AdaptiveAccessControl load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new AdaptiveAccessControl(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static AdaptiveAccessControl load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new AdaptiveAccessControl(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<Tuple2<BigInteger, BigInteger>> calculateDecision(Boolean abacPass, Boolean bookingActive, BigInteger resourceSensitivity, BigInteger operation, BigInteger trustScore, BigInteger riskScore) {
        final Function function = new Function(FUNC_CALCULATEDECISION,
                Arrays.<Type>asList(
                        new Bool(abacPass),
                        new Bool(bookingActive),
                        new Uint8(resourceSensitivity),
                        new Uint8(operation),
                        new Uint8(trustScore),
                        new Uint8(riskScore)
                ),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Uint8>() {},
                        new TypeReference<Uint8>() {}
                ));
        return new RemoteFunctionCall<Tuple2<BigInteger, BigInteger>>(function,
                () -> {
                    List<Type> results = executeCallMultipleValueReturn(function);
                    return new Tuple2<BigInteger, BigInteger>(
                            (BigInteger) results.get(0).getValue(),
                            (BigInteger) results.get(1).getValue());
                });
    }

    public RemoteFunctionCall<TransactionReceipt> evaluateAccess(byte[] requestReference, byte[] deviceIdentifierHash, Boolean abacPass, Boolean bookingActive, BigInteger resourceSensitivity, BigInteger operation, BigInteger trustScore, BigInteger riskScore) {
        final Function function = new Function(
                FUNC_EVALUATEACCESS,
                Arrays.<Type>asList(
                        new Bytes32(requestReference),
                        new Bytes32(deviceIdentifierHash),
                        new Bool(abacPass),
                        new Bool(bookingActive),
                        new Uint8(resourceSensitivity),
                        new Uint8(operation),
                        new Uint8(trustScore),
                        new Uint8(riskScore)
                ),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> trustHigh() {
        final Function function = new Function(FUNC_TRUSTHIGH,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> trustMedium() {
        final Function function = new Function(FUNC_TRUSTMEDIUM,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> riskLow() {
        final Function function = new Function(FUNC_RISKLOW,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> riskMedium() {
        final Function function = new Function(FUNC_RISKMEDIUM,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setTrustThresholds(BigInteger _trustHigh, BigInteger _trustMedium) {
        final Function function = new Function(
                FUNC_SETTRUSTTHRESHOLDS,
                Arrays.<Type>asList(new Uint8(_trustHigh), new Uint8(_trustMedium)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRiskThresholds(BigInteger _riskLow, BigInteger _riskMedium) {
        final Function function = new Function(
                FUNC_SETRISKTHRESHOLDS,
                Arrays.<Type>asList(new Uint8(_riskLow), new Uint8(_riskMedium)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static class AuthorizationEvaluatedEventResponse extends org.web3j.protocol.core.methods.response.BaseEventResponse {
        public byte[] requestReference;
        public byte[] deviceIdentifierHash;
        public BigInteger decision;
        public BigInteger trustScore;
        public BigInteger riskScore;
        public BigInteger resourceSensitivity;
        public BigInteger operation;
        public BigInteger timestamp;
    }

    public static List<AuthorizationEvaluatedEventResponse> getAuthorizationEvaluatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(AUTHORIZATIONEVALUATED_EVENT, transactionReceipt);
        ArrayList<AuthorizationEvaluatedEventResponse> responses = new ArrayList<AuthorizationEvaluatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AuthorizationEvaluatedEventResponse typedResponse = new AuthorizationEvaluatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requestReference = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.deviceIdentifierHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.decision = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.trustScore = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.riskScore = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.resourceSensitivity = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.operation = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }
}
