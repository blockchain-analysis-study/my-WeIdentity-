/*
 *       Copyright© (2018) WeBank Co., Ltd.
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

import lombok.Data;

/**
 * The base data structure for public key properties.
 *
 * @author tonychen 2018.10.8
 */
@Data
public class PublicKeyProperty {

    /**
     * todo 索引Id 格式 `WeId#keys-{index}` 如: did:example:123456789abcdefghi#keys-2
     * Required: The id.
     */
    private String id;

    /**
     * Required: The type.
     */
    private String type;

    /**
     * todo  PubKey 的拥有者
     * Required: The owner.
     */
    private String owner;

    /**
     * PubKey
     * Required: The public key.
     */
    private String publicKey;
}
