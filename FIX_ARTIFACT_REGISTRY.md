# Solución: Error de Artifact Registry

## Error
```
Permission "artifactregistry.repositories.downloadArtifacts" denied
```

## Solución

### Agregar rol: Artifact Registry Reader

1. **Ve a Google Cloud IAM:**
   ```
   https://console.cloud.google.com/iam-admin/iam?project=fastmind-c6603
   ```

2. **Busca o agrega la cuenta de servicio:**
   ```
   406678650879-compute@developer.gserviceaccount.com
   ```

3. **Agrega el rol:**
   - **Artifact Registry Reader**
   - O busca: `roles/artifactregistry.reader`

## Permisos Completos Necesarios

Para que Firebase Functions funcione correctamente, la cuenta de servicio necesita:

1. ✅ **Storage Object Viewer** (`roles/storage.objectViewer`)
2. ✅ **Logs Writer** (`roles/logging.logWriter`)
3. ⚠️ **Artifact Registry Reader** (`roles/artifactregistry.reader`) - **AGREGAR**

## Después de agregar el permiso

Ejecuta nuevamente:
```powershell
npx firebase deploy --only functions
```





