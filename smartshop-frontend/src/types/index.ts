/**
 * Wire contracts mirroring the Spring Boot DTOs in `smartshop-backend`.
 *
 * Two backend-wide rules shape everything below:
 *  1. `spring.jackson.default-property-inclusion=non_null` — a null field is an
 *     ABSENT key, never `null`. Model those as `field?: T` and read with
 *     `?? fallback` / `!field`, never `field === null`.
 *  2. Lombok getter naming decides the wire key for booleans: a primitive
 *     `boolean isActive` serialises as `active`, a wrapper `Boolean isActive`
 *     serialises as `isActive`. Both spellings appear below on purpose.
 *
 * Every controller wraps its body in `ApiResponse<T>` EXCEPT `TaxController`,
 * which returns raw DTOs, and `FileUploadController`, which returns `{ url }`.
 */

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: { field?: string; message: string }[];
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  /** 0-based. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/* ------------------------------------------------------------------ *
 * Shared enums (verbatim backend constants)
 * ------------------------------------------------------------------ */

export type Status = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED';
export type ShopStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
/** Branches only ever toggle between these two — there is no SUSPENDED. */
export type BranchStatus = 'ACTIVE' | 'INACTIVE';
export type UserStatus = 'ACTIVE' | 'BLOCKED' | 'SUSPENDED';

export type PaymentMethod = 'CASH' | 'CARD' | 'ESEWA' | 'KHALTI' | 'BANK_TRANSFER' | 'OTHER';
/** Document-level rollup on a sale/purchase. */
export type PaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID';
/** Per-transaction state on an individual payment row. */
export type PaymentState = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

export const PAYMENT_METHODS: PaymentMethod[] = [
  'CASH',
  'CARD',
  'ESEWA',
  'KHALTI',
  'BANK_TRANSFER',
  'OTHER',
];

export const PAYMENT_STATUSES: PaymentStatus[] = ['UNPAID', 'PARTIAL', 'PAID'];

export type MovementType =
  | 'PURCHASE_IN'
  | 'SALE_OUT'
  | 'SALE_RETURN_IN'
  | 'PURCHASE_RETURN_OUT'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'OPENING_STOCK';

export const MOVEMENT_TYPES: MovementType[] = [
  'PURCHASE_IN',
  'SALE_OUT',
  'SALE_RETURN_IN',
  'PURCHASE_RETURN_OUT',
  'ADJUSTMENT_IN',
  'ADJUSTMENT_OUT',
  'TRANSFER_IN',
  'TRANSFER_OUT',
  'OPENING_STOCK',
];

/* ------------------------------------------------------------------ *
 * Auth
 * ------------------------------------------------------------------ */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface BranchRoleGrant {
  branchId: string;
  branchName: string;
  shopId: string;
  role: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface AuthUser {
  accessToken?: string;
  tokenType: string;
  userId: string;
  email: string;
  fullName: string;
  roles: string[];
  branchRoles: BranchRoleGrant[];
  profileImageUrl?: string;
  emailVerified?: boolean;
}

/* ------------------------------------------------------------------ *
 * Shops & branches
 * ------------------------------------------------------------------ */

/** `ShopResponse`. Note: `panVatNumber`, not `vatNumber`; there is no `code`. */
export interface Shop {
  id: string;
  name: string;
  panVatNumber: string;
  phone?: string;
  email?: string;
  address?: string;
  logoUrl?: string;
  status: ShopStatus;
  createdAt: string;
}

/** `ShopRequest`. PUT reuses this DTO and is a FULL REPLACE — send every field. */
export interface ShopRequest {
  name: string;
  panVatNumber: string;
  phone?: string;
  email?: string;
  address?: string;
  logoUrl?: string;
  status?: ShopStatus;
}

/** `BranchResponse`. Note `contactNumber` (not `phone`) and `isMainBranch` (not `isMain`). */
export interface Branch {
  id: string;
  shopId: string;
  shopName: string;
  name: string;
  code: string;
  address?: string;
  contactNumber?: string;
  isMainBranch: boolean;
  status: BranchStatus;
  createdAt: string;
}

/**
 * `BranchRequest`. `code` is uppercased server-side and must be unique per shop.
 * `shopId` is `@NotNull` but ignored on update. Setting `isMainBranch: true`
 * clears the flag on every other branch in the shop.
 */
export interface BranchRequest {
  shopId: string;
  name: string;
  code: string;
  address?: string;
  contactNumber?: string;
  isMainBranch?: boolean;
  status?: BranchStatus;
}

/** `InviteCodeResponse`. Primitive `boolean isActive` → the wire key is `active`. */
export interface InviteCode {
  shopId: string;
  code: string;
  active: boolean;
  createdAt: string;
}

/* ------------------------------------------------------------------ *
 * Catalogue
 * ------------------------------------------------------------------ */

export interface Category {
  id: string;
  shopId: string;
  parentId?: string;
  name: string;
  code?: string;
  description?: string;
  status: Status;
  createdAt?: string;
  /** Only populated by `GET /categories/tree`; absent on single-item responses. */
  children?: Category[];
}

/** PUT reuses this DTO and is a FULL REPLACE — omitting `parentId` promotes to root. */
export interface CategoryRequest {
  shopId: string;
  parentId?: string | null;
  name: string;
  code?: string | null;
  description?: string | null;
  status?: Status;
}

export interface Product {
  id: string;
  shopId: string;
  categoryId?: string;
  categoryName?: string;
  name: string;
  sku: string;
  barcode?: string;
  brand?: string;
  unit: string;
  description?: string;
  purchasePrice: number;
  sellingPrice: number;
  /** Weighted average landed cost from inventory (includes allocated extra_cost). */
  effectiveCost?: number;
  /** Priced at the default target margin (20%). */
  suggestedSellingPrice?: number;
  profitMarginPercent?: number;
  vatApplicable: boolean;
  vatRate: number;
  taxId?: string;
  taxName?: string;
  reorderLevel: number;
  imageUrl?: string;
  status: Status;
  /** Always present; the only value the backend emits is the below-cost warning. */
  warnings?: string[];
  createdAt?: string;
}

/**
 * PUT reuses this DTO and is a FULL REPLACE. Omitted relations are CLEARED
 * (`categoryId`, `taxId`), and `shopId` may not change.
 */
export interface ProductRequest {
  shopId: string;
  categoryId?: string | null;
  name: string;
  sku: string;
  barcode?: string | null;
  brand?: string | null;
  unit?: string | null;
  description?: string | null;
  purchasePrice: number;
  sellingPrice: number;
  vatApplicable?: boolean;
  vatRate?: number | null;
  taxId?: string | null;
  reorderLevel?: number | null;
  imageUrl?: string | null;
  status?: Status;
}

export interface ProductSearch {
  id: string;
  name: string;
  sku: string;
  barcode?: string;
  unit: string;
  sellingPrice: number;
  vatApplicable: boolean;
  vatRate: number;
  quantityAvailable: number;
  stockStatus: string;
}

/* ------------------------------------------------------------------ *
 * Parties
 * ------------------------------------------------------------------ */

export interface Supplier {
  id: string;
  shopId: string;
  name: string;
  companyName?: string;
  phone?: string;
  email?: string;
  address?: string;
  panNumber?: string;
  status: Status;
  createdAt?: string;
}

/** PUT reuses this DTO and is a FULL REPLACE. */
export interface SupplierRequest {
  shopId: string;
  name: string;
  companyName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  panNumber?: string | null;
  status?: Status;
}

export interface Customer {
  id: string;
  shopId: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  loyaltyPoints: number;
  createdAt?: string;
}

/** PUT reuses this DTO and is a FULL REPLACE. */
export interface CustomerRequest {
  shopId: string;
  name: string;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
}

/* ------------------------------------------------------------------ *
 * Tax
 * ------------------------------------------------------------------ */

export type TaxType = 'PERCENTAGE' | 'FIXED';

export interface TaxRate {
  id: string;
  taxId: string;
  rate: number;
  validFrom: string;
  validTo?: string | null;
}

/** Wrapper `Boolean isActive` here → the wire key really is `isActive`. */
export interface Tax {
  id: string;
  shopId: string;
  shopName: string;
  name: string;
  type: TaxType;
  description?: string;
  isActive: boolean;
  rates: TaxRate[];
}

/* ------------------------------------------------------------------ *
 * Purchases
 * ------------------------------------------------------------------ */

export interface PurchaseItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  /** Purchase lines are costed with `unitCost`; sale lines use `unitPrice`. */
  unitCost: number;
  /** Transport, packaging, labor — allocated to the landed cost (pricing engine). */
  extraCost?: number;
  /** unitCost + extraCost */
  effectiveCost?: number;
  discountAmount?: number;
  vatRate?: number;
  vatAmount?: number;
  taxableAmount?: number;
  lineTotal: number;
}

export interface Purchase {
  id: string;
  purchaseNumber: string;
  purchaseDate: string;
  shopId: string;
  branchId: string;
  branchName: string;
  branchCode?: string;
  supplierId?: string;
  supplierName: string;
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  vatAmount: number;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  notes?: string;
  createdById?: string;
  createdByName: string;
  createdAt?: string;
  items?: PurchaseItem[];
}

/* ------------------------------------------------------------------ *
 * Sales
 * ------------------------------------------------------------------ */

export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  /** Snapshot of the inventory weighted-average cost when the sale was made. */
  unitCostAtSale?: number;
  /** Taxable revenue minus COGS for the line. */
  lineProfit?: number;
  discountAmount?: number;
  vatRate?: number;
  vatAmount?: number;
  taxableAmount?: number;
  lineTotal: number;
}

export interface Sale {
  id: string;
  invoiceNumber: string;
  billDate: string;
  shopId: string;
  branchId: string;
  branchName: string;
  branchCode?: string;
  customerId?: string;
  customerName?: string;
  customerPhone?: string;
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  vatAmount: number;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  qrReference?: string;
  /**
   * Declared on `SaleResponse` but never populated by `SaleService.toResponse()`,
   * so in practice these keys are always absent. Guard before rendering.
   */
  cashTendered?: number;
  changeAmount?: number;
  cashierId?: string;
  cashierName: string;
  remarks?: string;
  createdAt?: string;
  items?: SaleItem[];
}

/** Body for `PUT /sales/{id}/payment-status` and `PUT /purchases/{id}/payment-status`. */
export interface PaymentStatusRequest {
  paymentStatus: PaymentStatus;
}

/* ------------------------------------------------------------------ *
 * Payments
 * ------------------------------------------------------------------ */

export interface PaymentRequest {
  saleId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
}

/**
 * `paymentStatus` here is the per-transaction `PaymentState`
 * (PENDING/COMPLETED/FAILED/REFUNDED) — NOT the document-level `PaymentStatus`.
 */
export interface Payment {
  id: string;
  saleId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentState;
  referenceNumber?: string;
  paidAt: string;
  receivedById: string;
  receivedByName: string;
}

/** `paymentDate` is `@PastOrPresent`; send `YYYY-MM-DD`. */
export interface PurchasePaymentRequest {
  paymentDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
}

export interface PurchasePayment {
  id: string;
  purchaseId: string;
  paymentDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
}

/* ------------------------------------------------------------------ *
 * Purchase returns
 * ------------------------------------------------------------------ */

/** Exactly three fields — the server reads the cost off the referenced purchase item. */
export interface PurchaseReturnItemRequest {
  purchaseItemId: string;
  productId: string;
  quantity: number;
}

export interface PurchaseReturnRequest {
  purchaseId: string;
  branchId: string;
  returnDate?: string;
  reason?: string;
  items: PurchaseReturnItemRequest[];
}

export interface PurchaseReturnItem {
  id: string;
  purchaseItemId: string;
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitCost: number;
  lineTotal: number;
}

export interface PurchaseReturn {
  id: string;
  returnNumber: string;
  returnDate: string;
  purchaseId: string;
  purchaseNumber: string;
  branchId: string;
  branchName: string;
  reason?: string;
  refundAmount: number;
  createdById: string;
  createdByName: string;
  createdAt: string;
  items: PurchaseReturnItem[];
}

/* ------------------------------------------------------------------ *
 * Inventory & stock
 * ------------------------------------------------------------------ */

export interface InventoryItem {
  id: string;
  branchId: string;
  productId: string;
  productName: string;
  sku: string;
  quantityAvailable: number;
  quantityReserved: number;
  averageCost: number;
  reorderLevel: number;
  stockStatus: string;
}

/**
 * `quantity` is always positive — derive direction from the movement type
 * (`PURCHASE_IN`, `..._IN` add stock; `..._OUT` remove it).
 */
export interface StockMovement {
  id: string;
  branchId: string;
  productId: string;
  productName: string;
  sku: string;
  movementType: MovementType;
  quantity: number;
  /** Free-form: PURCHASE | SALE | PURCHASE_RETURN | SALE_RETURN | TRANSFER. */
  referenceType?: string;
  referenceId?: string;
  note?: string;
  createdByName?: string;
  createdAt: string;
}

/* ------------------------------------------------------------------ *
 * Expenses
 * ------------------------------------------------------------------ */

export interface Expense {
  id: string;
  shopId: string;
  branchId?: string;
  branchName?: string;
  title: string;
  category?: string;
  amount: number;
  expenseDate: string;
  paymentMethod?: PaymentMethod;
  note?: string;
  createdById?: string;
  createdByName: string;
  createdAt?: string;
}

/** Master list for the expense category dropdown (GET /api/v1/expense-categories). */
export interface ExpenseCategory {
  id: string;
  name: string;
  description?: string;
}

/** PUT reuses this DTO and is a FULL REPLACE. `amount` must be >= 0.01. */
export interface ExpenseRequest {
  shopId: string;
  branchId?: string | null;
  title: string;
  category?: string | null;
  amount: number;
  expenseDate?: string | null;
  paymentMethod?: PaymentMethod | null;
  note?: string | null;
}

/* ------------------------------------------------------------------ *
 * Reporting
 * ------------------------------------------------------------------ */

export interface DashboardData {
  date: string;
  totalSales: number;
  salesCount: number;
  totalPurchases: number;
  totalExpenses: number;
  grossProfit: number;
  /** Net profit = gross profit minus total expenses for the day. */
  netProfit: number;
  lowStockCount: number;
  /** Products with stock on hand but zero sales in the last 90 days. */
  slowMovingCount: number;
  topProducts: { productId: string; productName: string; sku: string; quantitySold: number; revenue: number }[];
  /** Sparse — iterate `PAYMENT_METHODS` and default missing keys to 0. */
  paymentBreakdown: Record<string, number>;
}

/**
 * Flat scalars only. `dateFrom`/`dateTo` default INDEPENDENTLY to today on the
 * server, so always send both. `totalSales` is VAT-inclusive and already net of
 * discount — do not subtract `totalDiscount` again.
 */
export interface SalesSummary {
  dateFrom: string;
  dateTo: string;
  totalSales: number;
  totalVat: number;
  totalDiscount: number;
  totalCogs: number;
  grossProfit: number;
  /** Total operating expenses in the period. */
  totalExpenses: number;
  /** Net profit = gross profit minus total expenses. */
  netProfit: number;
  saleCount: number;
}

export interface CategoryProfitRow {
  categoryId: string;
  categoryName: string;
  totalRevenue: number;
  totalCogs: number;
  totalProfit: number;
  profitMarginPct: number;
  totalQty: number;
}

export interface CategoryProfitReport {
  dateFrom: string;
  dateTo: string;
  categories: CategoryProfitRow[];
}

export interface ExpenseSummaryReport {
  dateFrom: string;
  dateTo: string;
  totalExpenses: number;
  byCategory: Record<string, number>;
}

export interface InventoryValuationItem {
  branchId: string;
  branchName: string;
  productId: string;
  productName: string;
  sku: string;
  quantityAvailable: number;
  averageCost: number;
  totalValue: number;
}

/**
 * Recommendation engine result. `FAST_MOVING` / `HIGH_PROFIT` fill the sales
 * metrics; `SLOW_MOVING` fills `status` (+ `lastSaleDate`); `REORDER` fills the
 * stock/reorder/urgency fields plus supplier.
 */
export interface ProductRecommendation {
  productId: string;
  productName: string;
  sku: string;
  unit?: string;
  quantitySold?: number;
  revenue?: number;
  profit?: number;
  marginPercent?: number;
  currentStock?: number;
  reorderLevel?: number;
  suggestedOrderQty?: number;
  avgDailySales?: number;
  score?: number;
  urgency?: number;
  status?: string;
  lastSaleDate?: string;
  suggestedAction?: string;
  supplierId?: string;
  supplierName?: string;
}

export type RecommendationType = 'FAST_MOVING' | 'HIGH_PROFIT' | 'SLOW_MOVING' | 'REORDER';

/* ------------------------------------------------------------------ *
 * Users, roles & assignments
 * ------------------------------------------------------------------ */

export interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  profileImageUrl?: string;
  status: string;
  emailVerified?: boolean;
  branchRoles: BranchRoleGrant[];
  createdAt: string;
}

/** `UserBranchRoleRef` — a shop-wide grant omits `branchId`/`branchName`. */
export interface UserBranchRoleRef {
  branchId?: string;
  branchName?: string;
  shopId?: string;
  role: string;
}

/** `UserResponse` from `/users`, `/users/me` and `/users/{id}`. */
export interface User {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  profileImageUrl?: string;
  status: UserStatus;
  emailVerified?: boolean;
  /** Inactive assignments are already filtered out server-side. */
  branchRoles: UserBranchRoleRef[];
  createdAt: string;
}

/**
 * `PATCH /users/me` is asymmetric: `firstName`/`lastName` are applied only when
 * non-blank (so `''` is a silent no-op), but `phone`/`profileImageUrl` are
 * applied whenever present (so `''` CLEARS them). Send only dirty fields.
 */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  profileImageUrl?: string;
}

/** Body for `PATCH /users/{id}/status` (SUPER_ADMIN only). */
export interface UserStatusRequest {
  status: UserStatus;
}

export interface RoleRef {
  id: string;
  name: string;
  description?: string;
}

/** Post the role's UUID, not its name. `shopId` is required even with `branchId`. */
export interface UserBranchRoleRequest {
  userId: string;
  shopId: string;
  branchId?: string | null;
  roleId: string;
}

/**
 * The list endpoint does NOT filter revoked rows — filter on `isActive` client
 * side. Re-granting a revoked combination 409s; there is no reactivate endpoint.
 * Wrapper `Boolean isActive` here → the wire key really is `isActive`.
 */
export interface UserBranchRole {
  id: string;
  userId: string;
  userFullName: string;
  shopId: string;
  shopName: string;
  branchId?: string;
  branchName?: string;
  roleId: string;
  roleName: string;
  isActive: boolean;
  createdAt: string;
}

/* ------------------------------------------------------------------ *
 * Audit
 * ------------------------------------------------------------------ */

export const AUDIT_ACTIONS = [
  'CREATE',
  'UPDATE',
  'DELETE',
  'APPROVE',
  'REJECT',
  'ROLE_ASSIGN',
  'ROLE_REVOKE',
] as const;

export const AUDIT_ENTITIES = [
  'Branch',
  'Product',
  'Purchase',
  'PurchasePayment',
  'Sale',
  'Settings',
  'Shop',
  'StockTransfer',
  'UserBranchRole',
] as const;

/**
 * `action` and `entityName` are plain strings on the wire (not enums) and the
 * server filters them with an exact, case-sensitive match. `oldValue`/`newValue`
 * are free-form text — never `JSON.parse` them. `userId`/`userName` are absent
 * together for system-generated rows.
 */
export interface AuditLog {
  id: string;
  userId?: string;
  userName?: string;
  action: string;
  entityName?: string;
  entityId?: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  createdAt: string;
}

/* ------------------------------------------------------------------ *
 * Onboarding
 * ------------------------------------------------------------------ */

export type RegistrationStatusValue = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ShopRegistration {
  id: string;
  userId: string;
  applicantName: string;
  applicantEmail: string;
  shopName: string;
  panVatNumber: string;
  panCertificateUrl?: string | null;
  phone?: string;
  address?: string;
  status: RegistrationStatusValue;
  rejectionReason?: string | null;
  createdAt: string;
}

export interface ShopSalesSummary {
  shopId: string;
  shopName: string;
  totalSales: number;
}

export interface StaffInvitation {
  id: string;
  userId: string;
  applicantName: string;
  applicantEmail: string;
  shopId: string;
  shopName: string;
  branchId?: string | null;
  inviteCode: string;
  requestedRole: string;
  status: RegistrationStatusValue;
  rejectionReason?: string | null;
  createdAt: string;
}
