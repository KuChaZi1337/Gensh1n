//
// MessagePack for Java
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package dev.undefinedteam.gensh1n.protocol.heypixel.msgpack.value;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Representation of MessagePack's Map type.
 *
 * MessagePack's Map type can represent sequence of key-value pairs.
 */
public interface MapValue
        extends Value
{
    /**
     * Returns number of key-value pairs in this array.
     */
    int size();

    Set<Value> keySet();

    Set<Map.Entry<Value, Value>> entrySet();

    Collection<Value> values();

    /**
     * Returns the value as {@code Map}.
     */
    Map<Value, Value> map();

    /**
     * Returns the key-value pairs as an array of {@code Value}.
     *
     * Odd elements are keys. Next element of an odd element is a value corresponding to the key.
     *
     * For example, if this value represents <code>{"k1": "v1", "k2": "v2"}</code>, this method returns ["k1", "v1", "k2", "v2"].
     */
    Value[] getKeyValueArray();
}
