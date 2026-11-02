plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("modCommon") {
            id = "mod-common"
            implementationClass = "ModCommonPlugin"
        }
    }
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.9.8")
    compileOnly("net.neoforged:moddev-gradle:2.0.148")
    implementation("me.modmuss50.mod-publish-plugin:me.modmuss50.mod-publish-plugin.gradle.plugin:2.2.1")
}
