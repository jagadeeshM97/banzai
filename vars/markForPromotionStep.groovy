#!/usr/bin/env groovy

def call(config) {

  if (config.markForPromotion) {
    if (config.markForPromotion && BRANCH_NAME !=~ config.markForPromotion) {
      logger "${BRANCH_NAME} does not match the markForPromotion pattern. Skipping"
      return 
    }

    try {
        notify(config, 'MarkForPromotion', 'Pending', 'PENDING', true)
        markForPromotion(config)
        passStep('MARK FOR PROMOTION')
        notify(config, 'MarkForPromotion', 'Successful', 'SUCCESS', true)
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, 'MarkForPromotion', 'Failed', 'FAILURE', true)
        error(err.message)
    }
  }

}
