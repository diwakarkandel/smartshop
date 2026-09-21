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
  Stack,
} from '@mui/material';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId } from '../stores/shopStore';
import type { Category, Status } from '../types';

const STATUSES: Status[] = ['ACTIVE', 'INACTIVE', 'DISCONTINUED'];

function CategoryNode({
  category,
  depth,
  onEdit,
  onDelete,
}: {
  category: Category;
  depth: number;
  onEdit: (c: Category) => void;
  onDelete: (c: Category) => void;
}) {
  const [open, setOpen] = useState(depth === 0);
  const hasChildren = Boolean(category.children?.length);
  return (
    <>
      <ListItem
        sx={{ pl: depth * 3 }}
        secondaryAction={
          <Stack direction="row" spacing={0.5}>
            <IconButton size="small" onClick={() => onEdit(category)}>
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" onClick={() => onDelete(category)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        }
      >
        <IconButton size="small" onClick={() => setOpen(!open)} disabled={!hasChildren}>
          {hasChildren ? open ? <ExpandLess /> : <ExpandMore /> : null}
        </IconButton>
        <ListItemText primary={category.name} secondary={category.code} />
        <Chip
          size="small"
          label={category.status}
          color={category.status === 'ACTIVE' ? 'success' : 'default'}
          sx={{ mr: 6 }}
        />
      </ListItem>
      {hasChildren && (
        <Collapse in={open} timeout="auto" unmountOnExit>
          {category.children!.map((child) => (
            <CategoryNode key={child.id} category={child} depth={depth + 1} onEdit={onEdit} onDelete={onDelete} />
          ))}
        </Collapse>
      )}
    </>
  );
}

const EMPTY_FORM = { name: '', code: '', parentId: '', description: '', status: 'ACTIVE' as Status };

export default function CategoriesPage() {
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Category | null>(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });

  const { data, isLoading } = useQuery({
    queryKey: ['categories', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Category[] }>('/categories/tree', { params: { shopId } });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const openNew = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setOpen(true);
  };

  const openEdit = (c: Category) => {
    setEditing(c);
    setForm({
      name: c.name,
      code: c.code ?? '',
      parentId: c.parentId ?? '',
      description: c.description ?? '',
      status: c.status,
    });
    setOpen(true);
  };

  const closeDialog = () => {
    setOpen(false);
    setEditing(null);
    setForm({ ...EMPTY_FORM });
  };

  const save = useMutation({
    mutationFn: async () => {
      const payload = {
        shopId,
        name: form.name,
        code: form.code || null,
        parentId: form.parentId || null,
        description: form.description || null,
        status: form.status,
      };
      return editing ? api.put(`/categories/${editing.id}`, payload) : api.post('/categories', payload);
    },
    onSuccess: () => {
      closeDialog();
      queryClient.invalidateQueries({ queryKey: ['categories'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/categories/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['categories'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const onDelete = (c: Category) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return;
    remove.mutate(c.id);
  };

  // Parent options: top-level categories, excluding the one being edited.
  const parentOptions = (data ?? []).filter((c) => c.id !== editing?.id);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Categories</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
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
              <CategoryNode key={c.id} category={c} depth={0} onEdit={openEdit} onDelete={onDelete} />
            ))}
            {(data ?? []).length === 0 && (
              <ListItem>No categories yet</ListItem>
            )}
          </List>
        )}
      </Card>

      <Dialog open={open} onClose={closeDialog} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Category - ${editing.name}` : 'New Category'}</DialogTitle>
        <DialogContent>
          <TextField label="Name" fullWidth margin="dense" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="Code" fullWidth margin="dense" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <TextField label="Description" fullWidth margin="dense" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <TextField
            select
            label="Parent"
            fullWidth
            margin="dense"
            value={form.parentId}
            onChange={(e) => setForm({ ...form, parentId: e.target.value })}
          >
            <MenuItem value="">None</MenuItem>
            {parentOptions.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>
          {editing && (
            <TextField
              select
              label="Status"
              fullWidth
              margin="dense"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value as Status })}
            >
              {STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" disabled={!form.name || save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
