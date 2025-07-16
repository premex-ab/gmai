plugins {
    id("se.premex.gmai")
}

managedAi {
    autoInstall = false
    autoStart = false

    models {
        create("llama3") {
            version = "8b"
        }
    }
}

tasks.register("myAiTask") {
    group = "custom"
    description = "A custom task that uses managed AI"

    doLast {
        println("Running custom AI task...")
        println("AI environment should be ready!")
    }
}

// Test the task extension functionality
tasks.named("myAiTask") {
    // This should add dependencies on setupManagedAi and finalizedBy teardownManagedAi
    dependsOn("setupManagedAi")
    finalizedBy("teardownManagedAi")
}
