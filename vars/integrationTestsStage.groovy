#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiIntegrationTestsCfg
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.integrationTests == null) { return }

  def stageName = 'IT'
  BanzaiBaseStage banzaiStage = new BanzaiBaseStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )

  BanzaiIntegrationTestsCfg itCfg = findValueInRegexObject(cfg.integrationTests, BRANCH_NAME)
  banzaiStage.validate {
    if (itCfg == null) {
      return "${BRANCH_NAME} does not match a 'integrationTests' branch pattern. Skipping ${stageName}"
    }
  }
  
  banzaiStage.execute {
    if (itCfg.xvfb) {
      def screen = itCfg.xvfbScreen ?: '1800x900x24'

      wrap([$class: 'Xvfb', screen: screen]) {
          def script = itCfg.script ?: "integrationTests.sh"
          runScript(cfg, script)
      }
    } else {
      def script = itCfg.script ?: "integrationTests.sh"
      runScript(cfg, script)
    }
  }
}
