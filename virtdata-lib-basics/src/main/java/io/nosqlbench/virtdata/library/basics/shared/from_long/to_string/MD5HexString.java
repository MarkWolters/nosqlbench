/*
 * Copyright (c) 2022 nosqlbench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nosqlbench.virtdata.library.basics.shared.from_long.to_string;

import io.nosqlbench.virtdata.annotations.types.Categories;
import io.nosqlbench.virtdata.annotations.types.Category;
import io.nosqlbench.virtdata.annotations.types.Example;
import io.nosqlbench.virtdata.annotations.types.ThreadSafeMapper;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.LongFunction;

/**
 * Computes the MD5 digest of the byte image of the input long, and
 * returns it in hexadecimal String form.
 */
@Categories(Category.conversion)
@ThreadSafeMapper
public class MD5HexString implements LongFunction<String> {

    private final MessageDigest md5;
    private static final transient ThreadLocal<TLState> tl_state = ThreadLocal.withInitial(TLState::new);

    @Example({"MD5String()","Convert a long input to an md5 digest over its bytes, and then to a hexadecimal string."})
    public MD5HexString() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String apply(long value) {
        TLState state = tl_state.get();
        state.bytes.putLong(0,value);
        byte[] digest = md5.digest(state.bytes.array());
        String hexDigest = Hex.encodeHexString(digest);
        return hexDigest;
    }

    private final static class TLState {
        public final ByteBuffer bytes = ByteBuffer.allocate(16);
        public final MessageDigest md5;
        public TLState() {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
