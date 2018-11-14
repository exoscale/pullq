@Library('jenkins-pipeline') _

node {
  cleanWs()

  try {
    dir('src') {
      stage('SCM') { checkout scm }
      updateGithubCommitStatus('PENDING', "${env.WORKSPACE}/src")
      stage('build') { docker()  }
      stage('publish') { publish()  }
    }
  } catch (err) {
    currentBuild.result = 'FAILURE'
    throw err
  } finally {
    if (!currentBuild.result) {
      currentBuild.result = 'SUCCESS'
    }
    updateGithubCommitStatus(currentBuild.result, "${env.WORKSPACE}/src")
    cleanWs cleanWhenFailure: false
  }
}

def docker() {
  withCredentials([[$class: 'StringBinding', credentialsId: 'github-policy-token', variable: 'GITHUB_TOKEN'],]) {
      docker.withRegistry('https://registry.internal.exoscale.ch') {
      def clojure = docker.image('registry.internal.exoscale.ch/exoscale/clojure:bionic')
      clojure.inside("-u root --net=host -v /home/exec/.m2/repository:/root/.m2/repository") {
          sh """cat <<EOF>pullq.conf
exoscale aargau 2
exoscale bern 2
exoscale blobd 2
exoscale blobtool 2
exoscale cli 2
exoscale clostack 2
exoscale cloud-canary 1
exoscale cloudstack-dev-ansible 1
exoscale cloudstack-private 2
exoscale collectd-python-extra 1
exoscale community 1
exoscale confederatio 2
exoscale confederatio-uri 2
exoscale console 2
exoscale cs 2
exoscale doc 1
exoscale dockerfiles 1
exoscale egoscale 2
exoscale exoip 2
exoscale graphq 2
exoscale himpy 1
exoscale jlog 1
exoscale jura 2
exoscale kafkanary 2
exoscale kalzone 2
exoscale nidwald 2
exoscale obwald 2
exoscale oncall-private 1
exoscale pithos-confederatio 2
exoscale pithos-private 2
exoscale puppet 1
exoscale raven 2
exoscale reporter 2
exoscale riemann-grid 1
exoscale riemann-mysql 1
exoscale rpp-c 1
exoscale runstatus 2
exoscale runstatus-cli 2
exoscale runstatus-fingerd 2
exoscale terraform 1
exoscale terraform-provider-exoscale 2
exoscale testing-compute 1
exoscale toolbox 2
exoscale unterwald 2
exoscale vncproxy 2
exoscale wallis 2
exoscale warp 2
exoscale website 2
exoscale zlocker 2
"""
      sh "lein run"
      }
    }
  }
}

def publish() {
  publishHTML target: [
    allowMissing: false,
    alwaysLinkToLastBuild: false,
    keepAll: true,
    reportDir: 'build',
    reportFiles: 'index.html',
    reportName: 'Pull-Request queue'
  ]
}
