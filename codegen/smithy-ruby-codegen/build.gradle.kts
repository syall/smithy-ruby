/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

description = "Generates Ruby code from Smithy models"
extra["displayName"] = "Smithy :: Ruby :: Codegen"
extra["moduleName"] = "software.amazon.smithy.ruby.codegen"

plugins {
    `java-library`
    id("software.amazon.smithy").version("0.5.3")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"("software.amazon.smithy:smithy-cli:${rootProject.extra["smithyVersion"]}")
    }
}

dependencies {
    api("software.amazon.smithy:smithy-codegen-core:${rootProject.extra["smithyVersion"]}")
    api("software.amazon.smithy:smithy-rules-engine:${rootProject.extra["smithyVersion"]}")
    implementation("software.amazon.smithy:smithy-waiters:${rootProject.extra["smithyVersion"]}")
    implementation("software.amazon.smithy:smithy-protocol-test-traits:${rootProject.extra["smithyVersion"]}")
}

java.sourceSets["main"].java {
    srcDirs("model", "src/main/smithy")
}
