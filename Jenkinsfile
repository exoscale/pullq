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
      clojure.inside() {
          sh """cat <<EOF>pullq.conf
exoscale puppet 1
exoscale doc 1
exoscale dockerfiles 1
exoscale cloudstack-private 2
exoscale bern 2
exoscale obwald 2
exoscale website 2
exoscale egoscale 2
exoscale testing-compute 1
exoscale console 2
exoscale unterwald 2
exoscale toolbox 2
exoscale pithos-private 2
exoscale blobtool 2
exoscale blobd 2
exoscale wallis 2
exoscale cs 2
exoscale exoip 2
exoscale zlocker 2
exoscale confederatio 2
exoscale reporter 2
exoscale raven 2
exoscale clostack 2
exoscale cli 2
exoscale cloudstack-dev-ansible 1
exoscale terraform-provider-exoscale 2
"""
      sh "env LEIN_HOME=$PWD/src/lein lein run"
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
    reportName: 'Obwald documentation'
  ]
}
