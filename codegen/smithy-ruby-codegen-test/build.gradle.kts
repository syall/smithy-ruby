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

extra["displayName"] = "Smithy :: Ruby :: Codegen :: Test"
extra["moduleName"] = "software.amazon.smithy.ruby.codegen.test"

tasks["jar"].enabled = false

plugins {
    id("software.amazon.smithy").version("0.5.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
    mavenCentral()
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
    implementation(project(":smithy-ruby-codegen"))
    implementation(project(":smithy-ruby-codegen-test-utils"))
}

tasks.register<Copy>("copyWhiteLabelGem") {
    mustRunAfter("copyIntegrationSpecs")
    mustRunAfter("copySteepfile")
    from("$buildDir/smithyprojections/smithy-ruby-codegen-test/white-label/ruby-codegen")
    into("$buildDir/../../projections/")
}
tasks.register<Copy>("copyWeatherServiceGem") {
    from("$buildDir/smithyprojections/smithy-ruby-codegen-test/weather-service/ruby-codegen")
    into("$buildDir/../../projections/")
}
tasks.register<Copy>("copyIntegrationSpecs") {
    from("./integration-specs")
    into("$buildDir/smithyprojections/smithy-ruby-codegen-test/white-label/ruby-codegen/white_label/spec")
}
tasks.register<Copy>("copySteepfile") {
    from("./Steepfile")
    into("$buildDir/smithyprojections/smithy-ruby-codegen-test/white-label/ruby-codegen/white_label")
}
tasks["build"].finalizedBy(
    tasks["copyIntegrationSpecs"],
    tasks["copySteepfile"],
    tasks["copyWhiteLabelGem"],
    tasks["copyWeatherServiceGem"]
)

java.sourceSets["main"].java {
    srcDirs("model", "src/main/smithy")
}
