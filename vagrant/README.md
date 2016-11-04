# peppol-smp-server Vagrant integration

This Vagrant setup was originally provided by [@jerouris](https://github.com/jerouris).

How to use it:
  * Download and install Vagrant from https://www.vagrantup.com/downloads.html
  * Download and install VirtualBox from https://www.virtualbox.org/wiki/Downloads
  * Have the SMP keystore JKS at hand and copy it into the `provision/keystore` folder
  * Copy `provision/keystore/keystore_vars.template.yml` to `provision/keystore/keystore_vars.yml`
  * Modify the created file `provision/keystore/keystore_vars.yml` with your keystore configuration
  * Run `vagrant up` and wait for the initial setup
  * Once it is finished, open `http://192.168.20.10:8080` in your browser
