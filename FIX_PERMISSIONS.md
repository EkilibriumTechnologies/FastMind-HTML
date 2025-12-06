# Solución: Error de Permisos en Firebase Functions

## Error
```
Access to bucket gcf-sources-406678650879-us-central1 denied
```

## Solución Rápida

### Opción 1: Desde Firebase Console (Recomendado)
1. Ve a: https://console.firebase.google.com/project/fastmind-c6603/settings/iam
2. Busca: `406678650879-compute@developer.gserviceaccount.com`
3. Si no existe, agrega el rol: **Storage Object Viewer**
4. Si ya existe, edítalo para agregar el rol

### Opción 2: Desde Google Cloud Console
1. Ve a: https://console.cloud.google.com/iam-admin/iam?project=fastmind-c6603
2. Busca o agrega: `406678650879-compute@developer.gserviceaccount.com`
3. Agrega el rol: **Storage Object Viewer** o **Storage Admin**

### Opción 3: Desde la Terminal (gcloud CLI)
```bash
gcloud projects add-iam-policy-binding fastmind-c6603 \
    --member="serviceAccount:406678650879-compute@developer.gserviceaccount.com" \
    --role="roles/storage.objectViewer"
```

## Después de agregar permisos
Ejecuta nuevamente:
```powershell
npx firebase deploy --only functions
```





