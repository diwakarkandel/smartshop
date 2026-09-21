import { useState, useRef } from 'react';
import { Box, CircularProgress, Typography, Avatar, IconButton } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../../lib/api';

interface ImageUploadProps {
  value?: string | null;
  onChange: (url: string | null) => void;
  label?: string;
  size?: number;
}

export default function ImageUpload({ value, onChange, label = 'Upload Image', size = 120 }: ImageUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setError('');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await api.post<{ url: string }>('/test/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      // The backend FileUploadController returns { url: ... } directly
      onChange(res.data.url);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setUploading(false);
      // Reset input so the same file can be selected again
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      
      {value ? (
        <Box sx={{ position: 'relative', display: 'inline-block' }}>
          <Avatar 
            src={value} 
            variant="rounded" 
            sx={{ width: size, height: size, bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider' }} 
          />
          <IconButton 
            size="small" 
            color="error" 
            sx={{ position: 'absolute', top: -10, right: -10, bgcolor: 'background.paper', boxShadow: 1 }}
            onClick={handleClear}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ) : (
        <Box 
          sx={{ 
            width: size, 
            height: size, 
            border: '1px dashed', 
            borderColor: 'divider', 
            borderRadius: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center',
            bgcolor: 'action.hover',
            cursor: uploading ? 'default' : 'pointer'
          }}
          onClick={() => !uploading && fileInputRef.current?.click()}
        >
          {uploading ? (
            <CircularProgress size={24} />
          ) : (
            <>
              <CloudUploadIcon color="action" sx={{ mb: 1 }} />
              <Typography variant="caption" color="text.secondary">Select File</Typography>
            </>
          )}
        </Box>
      )}
      
      <input
        type="file"
        ref={fileInputRef}
        style={{ display: 'none' }}
        accept="image/*"
        onChange={handleFileChange}
        disabled={uploading}
      />
      
      {error && (
        <Typography variant="caption" color="error">
          {error}
        </Typography>
      )}
    </Box>
  );
}
