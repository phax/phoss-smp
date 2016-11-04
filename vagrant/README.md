# peppol-smp-server Vagrant integration

This Vagrant setup was originally provided by [@jerouris](https://github.com/jerouris) - thanks a lot for this!

How to use it:
  * Download and install Vagrant from https://www.vagrantup.com/downloads.html
  * Download and install VirtualBox from https://www.virtualbox.org/wiki/Downloads
  * Have the SMP keystore JKS at hand and copy it into the `provision/keystore` folder
  * Copy `provision/keystore/keystore_vars.template.yml` to `provision/keystore/keystore_vars.yml`
  * Modify the created file `provision/keystore/keystore_vars.yml` with your keystore configuration and the unique SMP-ID
  * Run `vagrant up` and wait for the initial setup
  * Once it is finished, open `http://192.168.20.10:8080` in your browser
  * The default login is:
    * User: admin@helger.com
    * Password: password

## Using a proxy server

If a proxy server is needed to download all the relevant information, the following steps are necessary:

  * Install the Vagrant plugin using `vagrant plugin install vagrant-proxyconf`
  * Modify `Vagrantfile` and add the following entries:
```
  config.proxy.http     = "http://1.2.3.4:8080"
  config.proxy.https    = "http://1.2.3.4:8080"
  config.proxy.no_proxy = "localhost,127.0.0.1"
```
  * Modify `playbook-xml.yml`:
    * Search the line containing `mvn clean install` and add the following parameters afterwards: ` -Dhttp.proxyHost=1.2.3.4 -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts="localhost|127.0.0.1" -Dhttps.proxyHost=1.2.3.4 -Dhttps.proxyPort=8080` (note: there is no `-Dhttps.nonProxyHosts`!)
