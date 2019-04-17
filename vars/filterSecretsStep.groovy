#!/usr/bin/env groovy

def call(config) {

  if (config.filterSecrets) {
    // determine if branch matches and filterSecrets branches
    def secretConfigKey = filterSecrets.keySet().find { it ==~ BRANCH_NAME }
    if (!secretConfigKey) {
        logger "filterSecrets does not contain an entry that matches the branch: ${BRANCH_NAME}"
        return
    }

    notify(config, 'Filter Secrets', 'Pending', 'PENDING')
    def secretConfig = filterSecrets[secretConfigKey]
    filterSecrets(secretConfig)
    passStep('Filter Secrets')
    notify(config, 'Filter Secrets', 'Successful', 'SUCCESS')
  }

}
