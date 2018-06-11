def call(Map params = [:]) {
    def baseDir = params.containsKey('baseDir') ? params.baseDir : "."
    def metadataFile = params.containsKey('metadataFile') ? params.metadataFile : "essentials.yml"
    def labels = params.containsKey('labels') ? params.labels : "docker && highmem"
    def pluginEvaluationOutcome = params.get("pluginEvaluationOutcome") ?: "failOnInvalid"

    node(labels) {
        stage("Checkout") {
            infra.checkout()
        }

        dir(baseDir) {
            def metadataPath = "${pwd()}/${metadataFile}"
            metadata = readYaml(file: metadataPath)

            def customBOM = "${pwd tmp: true}/custom.bom.yml"
            def customWAR = "${pwd tmp: true}/custom.war"
            def customWarURI = "file://" + customWAR

            stage("Build Custom WAR") {
                customWARPackager.build(metadataPath, customWAR, customBOM)
            }

            if (metadata.ath != null && !metadata.ath.disabled) {
                stage("Run ATH") {
                    dir("ath") {
                        def configFile = "ath-config.groovy"
                        writeFile file: configFile, text: "pluginEvaluationOutcome='${pluginEvaluationOutcome}'"
                        runATH jenkins: customWarURI, metadataFile: metadataPath, configFile: configFile
                    }
                }
            }

            if (metadata.pct != null && !metadata.pct.disabled) {
                stage("Run PCT") {
                    runPCT jenkins: customWarURI, metadataFile: metadataPath
                }
            }
        }
    }
}
