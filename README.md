# Nexus provisioner

Provision nexus from a config file.

Currently using api v3.37. Allows to fetch initial admin password and secret texts using kuberenetes client with
sufficient privileges.

## Example config file

point to this file using the `NEXUS_PROVISIONER_CONFIG` env variable.

```yaml
# Debug output
debug: false
# Remove resources present on server but not in config
prune: true
# Enable kuberentes client
kubernetesClientEnabled: false

deployment:
  # Namespace & label selectors for nexus pod (to fetch initial pw) and secrets (to fetch secret refs)
  namespace: 'nexus'
  labelSelector: component=nexus
  # Nexus api uri
  apiUri: http://127.0.0.1:8081/service/rest/

# Whether to fetch the initial password from file in container
initAdminPassword: false
adminPassword:
  # Secret texts can be provided as clearText..
  clearText: admin
  # .. or as secretRef
  secretName: my-nexus-admin-password
  secretKey: admin-password

# See complete example in repo resources/config.yaml
```


