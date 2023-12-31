/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

/**
 * {@code Param} is used to signal that the annotated field should be
 * populated with a value retrieved from a parameter store through a {@link ParamProvider}.
 *
 * <p>By default {@code Param} use {@link SSMProvider} as parameter provider. This can be overridden specifying
 * the annotation variable {@code Param(provider = <Class-of-the-provider>)}.<br/>
 * The library provide a provider for AWS System Manager Parameters Store ({@link SSMProvider}) and a provider
 * for AWS Secrets Manager ({@link SecretsProvider}).
 * The user can implement a custom provider by extending the abstract class {@link BaseProvider}.</p>
 *
 * <p>If the parameter value requires transformation before being assigned to the annotated field
 * users can specify a {@link Transformer}
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {
    String key();

    Class<? extends BaseProvider> provider() default SSMProvider.class;

    Class<? extends Transformer> transformer() default Transformer.class;
}
