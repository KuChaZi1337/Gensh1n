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

/**
 * Immutable base interface of {@link ImmutableIntegerValue} and {@link ImmutableFloatValue} interfaces. To extract primitive type values, call toXXX methods, which may lose some information by rounding or truncation.
 *
 * @see ImmutableIntegerValue
 * @see ImmutableFloatValue
 */
public interface ImmutableNumberValue
        extends NumberValue, ImmutableValue
{
}
