plugins {
    alias(libs.plugins.agp.app) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
