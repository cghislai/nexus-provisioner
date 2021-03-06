# Nexus provisioner

Provision nexus from a config file.

Currently using api v3.37. Allows to fetch initial admin password and secret texts using kuberenetes client with
sufficient privileges.
- to fetch initial password, the client must list, get pods, and exec into it;
- to fetch secret references, the client must get secrets.
- to generate secrets, the client must write secrets.

## Example config file

point to this file using the `NEXUS_PROVISIONER_CONFIG` env variable.
see src/main/resources/config.yaml (https://github.com/cghislai/nexus-provisioner/blob/352984910e3472e3085e7bbe4435e6f42a1cd8b5/src/main/resources/config.yaml)

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
  # .. or reference a secret
  secretName: my-nexus-admin-password
  secretKey: admin-password
  # .. in which case the secret value can be generated if the secretKey (or secret) does not exists.
  generated: true

# See complete example in repo resources/config.yaml
```


