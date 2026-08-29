import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  Typography,
  List,
  ListItem,
  ListItemText,
  Collapse,
  IconButton,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Alert, MenuItem,
  CircularProgress,
} from '@mui/material';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { Category } from '../types';

function CategoryNode({ category, depth }: { category: Category; depth: number }) {
  const [open, setOpen] = useState(depth === 0);
  const hasChildren = Boolean(category.children?.length);
  return (
    <>
      <ListItem sx={{ pl: depth * 3 }}>
        <IconButton size="small" onClick={() => setOpen(!open)} disabled={!hasChildren}>
          {hasChildren ? open ? <ExpandLess /> : <ExpandMore /> : null}
        </IconButton>
        <ListItemText primary={category.name} secondary={category.code} />
        <Chip size="small" label={category.status} color={category.status === 'ACTIVE' ? 'success' : 'default'} />
      </ListItem>
      {hasChildren && (
        <Collapse in={open} timeout="auto" unmountOnExit>
          {category.children!.map((child) => (
            <CategoryNode key={child.id} category={child} depth={depth + 1} />
          ))}
        </Collapse>
      )}
    </>
  );
}

export default function CategoriesPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', code: '', parentId: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['categories', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Category[] }>('/categories/tree', { params: { shopId } });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const createCategory = useMutation({
    mutationFn: async () =>
      api.post('/categories', {
        shopId,
        name: form.name,
        code: form.code || null,
        parentId: form.parentId || null,
      }),
    onSuccess: () => {
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ['categories'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Categories</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Category
        </Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <List dense>
            {(data ?? []).map((c) => (
              <CategoryNode key={c.id} category={c} depth={0} />
            ))}
            {(data ?? []).length === 0 && (
              <ListItem>No categories yet</ListItem>
            )}
          </List>
        )}
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Category</DialogTitle>
        <DialogContent>
          <TextField label="Name" fullWidth margin="dense" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="Code" fullWidth margin="dense" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <TextField
            select
            label="Parent"
            fullWidth
            margin="dense"
            value={form.parentId}
            onChange={(e) => setForm({ ...form, parentId: e.target.value })}
          >
            <MenuItem value="">None</MenuItem>
            {(data ?? []).map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!form.name || createCategory.isPending} onClick={() => createCategory.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}