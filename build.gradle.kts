val minecraft_version: String by project
val mod_version: String by project
val maven_group: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project
val ktor_version: String by project
val kord_version: String by project
val fabric_kotlin_version: String by project
val archives_base_name: String by project

plugins {
	kotlin("jvm")
	id("fabric-loom")
	id("maven-publish")
	id("java")
	id("com.github.johnrengelman.shadow") version "7.0.0"
}

base {
	archivesName = archives_base_name
}

repositories {
	mavenCentral()
	maven { url = uri("https://maven.nucleoid.xyz/") }

	maven { url = uri("https://maven.isxander.dev/releases") }
}

dependencies {
	minecraft("com.mojang:minecraft:${minecraft_version}")
	mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
	modImplementation("net.fabricmc:fabric-loader:${loader_version}")

	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")

	implementation("com.neovisionaries:nv-websocket-client:2.14")?.let { shadow(it) }
	implementation("dev.kord:kord-core:$kord_version")?.let { shadow(it) }
	implementation("club.minnced:discord-webhooks:0.7.5")?.let { shadow(it) }

	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")
	modImplementation("dev.isxander:yet-another-config-lib:3.5.0+1.21-fabric")
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks {
	java {
		withSourcesJar()
	}

	withType(JavaCompile::class) {
		options.encoding = "UTF-8"
	}

	processResources {
		inputs.property("version", mod_version)

		from(project.the<SourceSetContainer>()["main"].resources.srcDirs) {
			include("fabric.mod.json")
			expand("version" to mod_version)

			duplicatesStrategy = DuplicatesStrategy.WARN
		}
	}

	remapJar {
		dependsOn(shadowJar)
		inputFile.set(shadowJar.get().archiveFile)
		archiveFileName.set("${project.name}-${mod_version}.jar")
	}

	shadowJar {
		configurations = listOf(project.configurations.shadow.get())
	}

	build {

	}
}
