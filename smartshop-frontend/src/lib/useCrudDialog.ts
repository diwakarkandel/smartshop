import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api, { extractErrorMessage } from './api';

export interface UseCrudDialogOptions<TItem, TForm> {
  /** REST collection path, e.g. '/customers'. Item ops hit `${path}/${id}`. */
  path: string;
  /** react-query key prefix to invalidate after any write, e.g. 'customers'. */
  invalidateKey: string;
  /** The blank form used when creating a new record. */
  emptyForm: TForm;
  /** Maps an existing record to form values when opening the edit dialog. */
  toForm: (item: TItem) => TForm;
  /** Builds the request body from the current form (create vs edit if needed). */
  toPayload: (form: TForm, editing: TItem | null) => unknown;
}

/**
 * Encapsulates the create/edit/delete dialog lifecycle shared by the CRUD pages
 * (Customers, Suppliers, …): open/edit/close state, the form buffer, and the
 * save (POST vs PUT) + delete mutations with query invalidation and error
 * capture. Pages keep their own table and field markup and just wire to this —
 * a single source of truth for the boilerplate (DRY / SRP).
 */
export function useCrudDialog<TItem extends { id: string }, TForm>(
  opts: UseCrudDialogOptions<TItem, TForm>,
) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TItem | null>(null);
  const [form, setForm] = useState<TForm>(opts.emptyForm);
  const [error, setError] = useState('');

  const openNew = () => {
    setEditing(null);
    setForm(opts.emptyForm);
    setOpen(true);
  };

  const openEdit = (item: TItem) => {
    setEditing(item);
    setForm(opts.toForm(item));
    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    setEditing(null);
    setForm(opts.emptyForm);
  };

  const invalidate = () => queryClient.invalidateQueries({ queryKey: [opts.invalidateKey] });

  const save = useMutation({
    mutationFn: async () => {
      const payload = opts.toPayload(form, editing);
      return editing
        ? api.put(`${opts.path}/${editing.id}`, payload)
        : api.post(opts.path, payload);
    },
    onSuccess: () => {
      close();
      setError('');
      invalidate();
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`${opts.path}/${id}`),
    onSuccess: invalidate,
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return { open, editing, form, setForm, error, setError, openNew, openEdit, close, save, remove };
}
