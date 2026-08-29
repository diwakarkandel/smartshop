import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  Typography,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Alert,
  Checkbox,
  FormControlLabel,
  CircularProgress,
  Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  createTax,
  updateTax,
  addTaxRate,
  listTaxes,
  extractErrorMessage,
  type TaxPayload,
  type TaxRatePayload,
} from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { Tax, TaxType } from '../types';

const today = (): string => {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
};

const rateSchema = z
  .object({
    rate: z.coerce.number().min(0, 'Rate must be 0 or more'),
    validFrom: z.string().min(1, 'Valid from is required'),
    validTo: z.string().optional(),
  })
  .refine((v) => !v.validTo || v.validFrom <= v.validTo, {
    message: 'Valid to must be on or after valid from',
    path: ['validTo'],
  });

const createSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  description: z.string().max(500).optional(),
  isActive: z.boolean(),
  rates: z.array(rateSchema).min(1, 'Add at least one rate'),
});

const editSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  description: z.string().max(500).optional(),
  isActive: z.boolean(),
});

const singleRateSchema = rateSchema;

type CreateFormValues = z.infer<typeof createSchema>;
type EditFormValues = z.infer<typeof editSchema>;
type RateFormValues = z.infer<typeof singleRateSchema>;

interface CreateTaxDialogProps {
  open: boolean;
  onClose: () => void;
  shopId: string;
}

function CreateTaxDialog({ open, onClose, shopId }: CreateTaxDialogProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<CreateFormValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      name: '',
      description: '',
      isActive: true,
      rates: [{ rate: 0, validFrom: today(), validTo: '' }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'rates' });
  const [saving, setSaving] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setError('');
    setSaving(true);
    try {
      const payload: TaxPayload = {
        shopId,
        name: values.name,
        type: 'PERCENTAGE',
        description: values.description || undefined,
        isActive: values.isActive,
        rates: values.rates.map((r) => ({
          rate: Number(r.rate),
          validFrom: r.validFrom,
          validTo: r.validTo || null,
        })),
      };
      await createTax(payload);
      reset();
      onClose();
      queryClient.invalidateQueries({ queryKey: ['taxes', shopId] });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New Tax</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        <Box component="form" onSubmit={onSubmit} noValidate>
          <TextField
            label="Name"
            fullWidth
            margin="dense"
            {...register('name')}
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
          />
          <TextField
            label="Description"
            fullWidth
            margin="dense"
            multiline
            minRows={2}
            {...register('description')}
            error={Boolean(errors.description)}
            helperText={errors.description?.message}
          />
          <FormControlLabel control={<Checkbox {...register('isActive')} />} label="Active" />
          <Typography variant="subtitle2" sx={{ mt: 1, mb: 0.5 }}>
            Rates
          </Typography>
          {fields.map((field, index) => (
            <Stack key={field.id} direction="row" spacing={1} alignItems="flex-start" sx={{ mb: 1 }}>
              <TextField
                label="Rate %"
                type="number"
                size="small"
                {...register(`rates.${index}.rate`)}
                error={Boolean(errors.rates?.[index]?.rate)}
                helperText={errors.rates?.[index]?.rate?.message}
                sx={{ width: 110 }}
              />
              <TextField
                label="Valid From"
                type="date"
                size="small"
                slotProps={{ inputLabel: { shrink: true } }}
                {...register(`rates.${index}.validFrom`)}
                error={Boolean(errors.rates?.[index]?.validFrom)}
                helperText={errors.rates?.[index]?.validFrom?.message}
              />
              <TextField
                label="Valid To"
                type="date"
                size="small"
                slotProps={{ inputLabel: { shrink: true } }}
                {...register(`rates.${index}.validTo`)}
                error={Boolean(errors.rates?.[index]?.validTo)}
                helperText={errors.rates?.[index]?.validTo?.message}
              />
              <IconButton size="small" onClick={() => remove(index)} aria-label="Remove rate">
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Stack>
          ))}
          {typeof errors.rates?.message === 'string' && (
            <Typography variant="caption" color="error">
              {errors.rates.message}
            </Typography>
          )}
          <Button
            size="small"
            startIcon={<AddIcon />}
            onClick={() => append({ rate: 0, validFrom: '', validTo: '' })}
          >
            Add rate
          </Button>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={saving} onClick={onSubmit}>
          {saving ? <CircularProgress size={20} color="inherit" /> : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface EditTaxDialogProps {
  open: boolean;
  onClose: () => void;
  tax: Tax | null;
  shopId: string;
}

function EditTaxDialog({ open, onClose, tax, shopId }: EditTaxDialogProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    values: {
      name: tax?.name ?? '',
      description: tax?.description ?? '',
      isActive: tax?.isActive ?? true,
    },
  });
  const [saving, setSaving] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    if (!tax) return;
    setError('');
    setSaving(true);
    try {
      const payload: TaxPayload = {
        shopId,
        name: values.name,
        type: tax.type,
        description: values.description || undefined,
        isActive: values.isActive,
        rates: [],
      };
      await updateTax(tax.id, payload);
      reset();
      onClose();
      queryClient.invalidateQueries({ queryKey: ['taxes', shopId] });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Edit Tax</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        <Box component="form">
          <TextField
            label="Name"
            fullWidth
            margin="dense"
            {...register('name')}
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
          />
          <TextField
            label="Description"
            fullWidth
            margin="dense"
            multiline
            minRows={2}
            {...register('description')}
            error={Boolean(errors.description)}
            helperText={errors.description?.message}
          />
          <FormControlLabel control={<Checkbox {...register('isActive')} />} label="Active" />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={saving} onClick={onSubmit}>
          {saving ? <CircularProgress size={20} color="inherit" /> : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface AddRateDialogProps {
  open: boolean;
  onClose: () => void;
  tax: Tax | null;
  shopId: string;
}

function AddRateDialog({ open, onClose, tax, shopId }: AddRateDialogProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RateFormValues>({
    resolver: zodResolver(singleRateSchema),
    defaultValues: { rate: 0, validFrom: today(), validTo: '' },
  });
  const [saving, setSaving] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    if (!tax) return;
    setError('');
    setSaving(true);
    try {
      const payload: TaxRatePayload = {
        rate: Number(values.rate),
        validFrom: values.validFrom,
        validTo: values.validTo || null,
      };
      await addTaxRate(tax.id, payload);
      reset();
      onClose();
      queryClient.invalidateQueries({ queryKey: ['taxes', shopId] });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{tax ? `Add Rate - ${tax.name}` : 'Add Rate'}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        <Box component="form">
          <TextField
            label="Rate %"
            type="number"
            fullWidth
            margin="dense"
            {...register('rate')}
            error={Boolean(errors.rate)}
            helperText={errors.rate?.message}
          />
          <TextField
            label="Valid From"
            type="date"
            fullWidth
            margin="dense"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('validFrom')}
            error={Boolean(errors.validFrom)}
            helperText={errors.validFrom?.message}
          />
          <TextField
            label="Valid To"
            type="date"
            fullWidth
            margin="dense"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('validTo')}
            error={Boolean(errors.validTo)}
            helperText={errors.validTo?.message}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={saving} onClick={onSubmit}>
          {saving ? <CircularProgress size={20} color="inherit" /> : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function formatRateWindow(rate: { rate: number; validFrom: string; validTo?: string | null }): string {
  const to = rate.validTo ? ` - ${rate.validTo}` : ' (open-ended)';
  return `${rate.rate}% from ${rate.validFrom}${to}`;
}

export default function TaxSettingsPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Tax | null>(null);
  const [addingRate, setAddingRate] = useState<Tax | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['taxes', shopId],
    queryFn: async () => listTaxes(shopId!, false),
    enabled: Boolean(shopId),
  });

  const toggleActive = useMutation({
    mutationFn: async (tax: Tax) =>
      updateTax(tax.id, {
        shopId: shopId!,
        name: tax.name,
        type: tax.type as TaxType,
        description: tax.description,
        isActive: !tax.isActive,
        rates: [],
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['taxes', shopId] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Tax Settings</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New Tax
        </Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {!shopId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Select a shop from the shop switcher to manage tax rates.
        </Alert>
      )}
      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Rates</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data ?? []).map((tax) => (
                <TableRow key={tax.id}>
                  <TableCell>
                    <Typography variant="body2">{tax.name}</Typography>
                    {tax.description && (
                      <Typography variant="caption" color="text.secondary">
                        {tax.description}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{tax.type}</TableCell>
                  <TableCell>
                    {tax.rates.length === 0 ? (
                      <Typography variant="caption" color="text.secondary">
                        No rates yet
                      </Typography>
                    ) : (
                      tax.rates.map((rate) => (
                        <Typography key={rate.id} variant="caption" display="block">
                          {formatRateWindow(rate)}
                        </Typography>
                      ))
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={tax.isActive ? 'success' : 'default'}
                      label={tax.isActive ? 'ACTIVE' : 'INACTIVE'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                      <IconButton size="small" aria-label="Add rate" onClick={() => setAddingRate(tax)}>
                        <AddCircleOutlineIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" aria-label="Edit tax" onClick={() => setEditing(tax)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        aria-label={tax.isActive ? 'Deactivate tax' : 'Activate tax'}
                        onClick={() => {
                          if (tax.isActive && !window.confirm(`Deactivate "${tax.name}"?`)) return;
                          toggleActive.mutate(tax);
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {(data ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>No tax rates found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>

      {shopId && (
        <CreateTaxDialog open={createOpen} onClose={() => setCreateOpen(false)} shopId={shopId} />
      )}
      <EditTaxDialog
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        tax={editing}
        shopId={shopId ?? ''}
      />
      <AddRateDialog
        open={Boolean(addingRate)}
        onClose={() => setAddingRate(null)}
        tax={addingRate}
        shopId={shopId ?? ''}
      />
    </Box>
  );
}