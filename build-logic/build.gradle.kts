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
    implementation("net.darkhax.curseforgegradle:net.darkhax.curseforgegradle.gradle.plugin:1.1.24")
    implementation("com.modrinth.minotaur:com.modrinth.minotaur.gradle.plugin:2.8.7")
}
