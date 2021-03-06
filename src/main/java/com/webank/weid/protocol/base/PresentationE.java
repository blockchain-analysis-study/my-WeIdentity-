/*
 *       Copyright© (2018-2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-java-sdk.
 *
 *       weid-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.protocol.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.exception.DataTypeCastException;
import com.webank.weid.protocol.inf.IProof;
import com.webank.weid.protocol.inf.RawSerializer;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;


/**
 * TODO 对外的 Credential  Presentation
 * Created by Junqi Zhang on 2019/4/4.
 */
@Data
@EqualsAndHashCode
public class PresentationE implements RawSerializer, IProof {

    private static final Logger logger = LoggerFactory.getLogger(PresentationE.class);

    /**
     * the serialVersionUID.
     */
    private static final long serialVersionUID = -595605743843891841L;

    /**
     * Required: The context field.
     */
    private List<String> context = new ArrayList<String>();

    private List<String> type = new ArrayList<String>();

    // todo 包含了对外的 CredentialPojo (注意不是 Credential哦)
    private List<CredentialPojo> verifiableCredential;

    private Map<String, Object> proof;

    /**
     * 获取公钥Id，用于验证的时候识别publicKey.
     * @return publicKeyId
     */
    public String getVerificationMethod() {
        return toString(getValueFromProof(proof, ParamKeyConstant.PROOF_VERIFICATION_METHOD));
    }
    
    /**
     * 获取challenge随机值.
     * @return 返回challenge随机值.
     */
    public String getNonce() {
        return toString(getValueFromProof(proof, ParamKeyConstant.PROOF_NONCE));
    }

    /**
     * 获取签名值Signature.
     * @return 返回签名字符串Signature.
     */
    public String getSignature() {
        return toString(getValueFromProof(proof, ParamKeyConstant.PROOF_SIGNATURE));
    }
    
    /**
     * 向proof中添加key-value.
     * @param key proof中的 key
     * @param value proof中key的value
     */
    public void putProofValue(String key, Object value) {
        if (proof == null) {
            proof = new HashMap<>();
        }
        proof.put(key, value);
    }
    
    /**
     * convert PresentationE to JSON String.
     * @return PresentationE
     */
    @Override
    public String toJson() {
        String json = DataToolUtils.convertTimestampToUtc(DataToolUtils.serialize(this));
        return DataToolUtils.addTagFromToJson(json);
    }
    
    /**
     * create PresentationE with JSON String.
     * @param presentationJson the presentation JSON String
     * @return PresentationE PresentationE
     */
    public static PresentationE fromJson(String presentationJson) {
        if (StringUtils.isBlank(presentationJson)) {
            logger.error("create PresentationE with JSON String failed, "
                + "the presentation JSON String is null");
            throw new DataTypeCastException("the presentation JSON String is null");
        }
        String presentationString = presentationJson;
        if (DataToolUtils.isValidFromToJson(presentationJson)) {
            presentationString = DataToolUtils.removeTagFromToJson(presentationJson);
        }
        PresentationE presentationE = DataToolUtils.deserialize(
            DataToolUtils.convertUtcToTimestamp(presentationString), 
            PresentationE.class);
        if (presentationE == null 
            || presentationE.getVerifiableCredential() == null 
            || presentationE.getVerifiableCredential().isEmpty()) {
            logger.error("create PresentationE with JSON String failed, "
                + "due to convert UTC to Timestamp error");
            throw new DataTypeCastException("convert UTC to Timestamp error");
        }
        for (CredentialPojo credentialPojo : presentationE.getVerifiableCredential()) {
            ErrorCode checkResp = CredentialPojoUtils.isCredentialPojoValid(credentialPojo);
            if (ErrorCode.SUCCESS.getCode() != checkResp.getCode()) {
                logger.error("create PresentationE with JSON String failed, {}", 
                    checkResp.getCodeDesc());
                throw new DataTypeCastException(checkResp.getCodeDesc());
            }
            if (!CredentialPojoUtils.validClaimAndSaltForMap(
                credentialPojo.getClaim(),  
                credentialPojo.getSalt())) {
                logger.error("create PresentationE with JSON String failed, claim and salt of "
                    + "credentialPojo not match.");
                throw new DataTypeCastException("claim and salt of credentialPojo not match.");
            }
        } 
        return presentationE;
    }
    
    /**
     * 将非policy里面的Credential添加到Presentation中
     * todo 调用 push(CredentialPojo credentialPojo)
     *      添加完所有Credential后需要调用 commit(WeIdAuthentication weIdAuthentication)
     *      进行重新签名，否则验证Presentation时会失败.
     * push the CredentialPojo into PresentationE.
     * @param credentialPojo the credential
     * @return true is success, others fail
     */
    public boolean push(CredentialPojo credentialPojo) {
        if (verifiableCredential == null || credentialPojo == null) {
            logger.error("[push] the credentialPojo is null.");
            return false;
        }
        ErrorCode checkResp = CredentialPojoUtils.isCredentialPojoValid(credentialPojo);
        if (ErrorCode.SUCCESS.getCode() != checkResp.getCode()) {
            logger.error("[push] the credentialPojo is invalid.");
            return false;
        }
        verifiableCredential.add(credentialPojo);
        logger.info("[push] the credentialPojo is added.");
        return true;
    }
    
    /**
     * todo 提交 Credential 进行 重新签名 (文档上说 push 完之后, 必须再次调用, 否则 验证Presentation时会失败)
     * commit the credential to sign.
     * @param weIdAuthentication the authentication
     * @return true is success, others fail
     */
    public boolean commit(WeIdAuthentication weIdAuthentication) {
        if (weIdAuthentication == null 
                || weIdAuthentication.getWeIdPrivateKey() == null
                || weIdAuthentication.getWeId() == null) {
            logger.error("[commit] the weIdAuthentication is null.");
            return false;
        }
        if (!WeIdUtils.validatePrivateKeyWeIdMatches(
                weIdAuthentication.getWeIdPrivateKey(), 
                weIdAuthentication.getWeId())) {
            logger.error("[commit] the private key does not match the current weid.");
            return false;
        }
        if (!weIdAuthentication.getWeIdPublicKeyId().equals(this.getVerificationMethod())) {
            logger.error("[commit] the public key id does not match the presentation.");
            return false;
        }
        // 更新proof里面的签名
        String signature = 
            DataToolUtils.sign(
                this.toRawData(),
                weIdAuthentication.getWeIdPrivateKey().getPrivateKey()
            );
        this.putProofValue(ParamKeyConstant.PROOF_SIGNATURE, signature);
        logger.info("[commit] commit credential with weIdAuthentication is success.");
        return true;
    }
    
    @Override
    public String toRawData() {
        PresentationE presentation = DataToolUtils.clone(this);
        presentation.proof = null;
        // 将 presentation的各个字段进行 序列化
        return DataToolUtils.serialize(presentation);
    }
}
