import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  IconButton, Badge, Menu, Box, Typography, Divider, List, ListItem,
  ListItemText, Chip, CircularProgress,
} from '@mui/material';
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import api from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { ROLES } from '../../lib/routeRoles';
import type { AuditLog, PageResponse } from '../../types';

function relativeTime(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const diff = Date.now() - then;
  const min = Math.floor(diff / 60000);
  if (min < 1) return 'just now';
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  return `${Math.floor(hr / 24)}d ago`;
}

const LAST_SEEN_KEY = 'ss-notif-last-seen';

function readLastSeen(): number {
  try {
    const v = localStorage.getItem(LAST_SEEN_KEY);
    return v ? Number(v) : 0;
  } catch {
    return 0;
  }
}

const ACTION_COLOR: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'error',
  PAYMENT_STATUS_CHANGE: 'warning',
  PRICE_CHANGE: 'warning',
  STATUS_CHANGE: 'warning',
};

export default function NotificationBell() {
  const hasRole = useAuthStore((s) => s.hasRole);
  // Audit logs are not yet shop-scoped, so restrict the cross-shop activity feed to
  // SUPER_ADMIN to avoid leaking one shop's activity to another shop's admin.
  const canSeeActivity = hasRole(ROLES.SUPER_ADMIN);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  // Timestamp (ms) of the newest notification the user has already seen. Persisted
  // so the badge stays cleared across reloads and only counts genuinely newer items.
  const [lastSeen, setLastSeen] = useState<number>(readLastSeen);

  const { data, isLoading } = useQuery({
    queryKey: ['notifications-recent'],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<AuditLog> }>('/audit-logs', {
        params: { page: 0, size: 8 },
      });
      return res.data.data.content;
    },
    enabled: canSeeActivity,
    refetchInterval: 60000,
    staleTime: 30000,
  });

  const items = data ?? [];

  // Unread = items newer than the last time the menu was opened.
  const unreadCount = useMemo(
    () => items.filter((n) => new Date(n.createdAt).getTime() > lastSeen).length,
    [items, lastSeen],
  );

  const openMenu = (e: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(e.currentTarget);
    // Viewing clears the badge: remember the newest item's time as "seen".
    if (items.length > 0) {
      const newest = Math.max(...items.map((n) => new Date(n.createdAt).getTime()));
      setLastSeen(newest);
      try {
        localStorage.setItem(LAST_SEEN_KEY, String(newest));
      } catch {
        /* private mode / storage blocked — badge still clears for this session */
      }
    }
  };

  return (
    <>
      <IconButton onClick={openMenu} aria-label="Notifications">
        <Badge badgeContent={unreadCount} color="error" max={9}>
          <NotificationsNoneIcon />
        </Badge>
      </IconButton>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { width: 360, maxWidth: '90vw' } } }}
      >
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="subtitle2" fontWeight={700}>Notifications</Typography>
          <Typography variant="caption" color="text.secondary">Recent activity</Typography>
        </Box>
        <Divider />
        {!canSeeActivity ? (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">You&apos;re all caught up.</Typography>
          </Box>
        ) : isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress size={22} />
          </Box>
        ) : items.length === 0 ? (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">No recent activity.</Typography>
          </Box>
        ) : (
          <List dense disablePadding sx={{ maxHeight: 380, overflowY: 'auto' }}>
            {items.map((n) => (
              <ListItem key={n.id} divider alignItems="flex-start">
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Chip
                        size="small"
                        label={n.action.replace(/_/g, ' ')}
                        color={ACTION_COLOR[n.action] ?? 'default'}
                        sx={{ height: 20, fontSize: 11 }}
                      />
                      <Typography variant="body2" fontWeight={600}>{n.entityName ?? '—'}</Typography>
                    </Box>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary">
                      {(n.userName ?? 'system')} · {relativeTime(n.createdAt)}
                      {n.newValue ? ` · ${n.newValue}` : ''}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </Menu>
    </>
  );
}
