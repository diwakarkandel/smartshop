  export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: { field?: string; message: string }[];
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

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

export interface AuthUser {
  accessToken?: string;
  tokenType: string;
  userId: string;
  email: string;
  fullName: string;
  roles: string[];
  branchRoles: BranchRoleGrant[];
  profileImageUrl?: string;
}

export interface Shop {
  id: string;
  name: string;
  code: string;
  vatNumber?: string;
  address?: string;
  phone?: string;
  email?: string;
  status: string;
}

export interface Branch {
  id: string;
  shopId: string;
  name: string;
  code: string;
  address?: string;
  phone?: string;
  isMain: boolean;
  status: string;
}

export interface Category {
  id: string;
  shopId: string;
  parentId?: string;
  name: string;
  code?: string;
  description?: string;
  status: string;
  children?: Category[];
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
  purchasePrice: number;
  sellingPrice: number;
  vatApplicable: boolean;
  vatRate: number;
  reorderLevel: number;
  status: string;
  warnings?: string[];
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

export interface Supplier {
  id: string;
  shopId: string;
  name: string;
  companyName?: string;
  phone?: string;
  email?: string;
  address?: string;
  panNumber?: string;
  status: string;
}

export interface Customer {
  id: string;
  shopId: string;
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  loyaltyPoints: number;
}

export interface Purchase {
  id: string;
  purchaseNumber: string;
  purchaseDate: string;
  shopId: string;
  branchId: string;
  branchName: string;
  supplierId: string;
  supplierName: string;
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  vatAmount: number;
  totalAmount: number;
  paymentStatus: string;
  createdByName: string;
}

export interface Sale {
  id: string;
  invoiceNumber: string;
  billDate: string;
  shopId: string;
  branchId: string;
  branchName: string;
  customerName?: string;
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  vatAmount: number;
  totalAmount: number;
  paymentStatus: string;
  paymentMethod?: string;
  cashTendered?: number;
  changeAmount?: number;
  cashierName: string;
  items?: SaleItem[];
}

export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  vatRate?: number;
  vatAmount?: number;
  taxableAmount?: number;
  lineTotal: number;
}

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

export interface Expense {
  id: string;
  shopId: string;
  branchId?: string;
  title: string;
  category?: string;
  amount: number;
  expenseDate: string;
  paymentMethod?: string;
  createdByName: string;
}

export interface DashboardData {
  date: string;
  totalSales: number;
  salesCount: number;
  totalPurchases: number;
  totalExpenses: number;
  grossProfit: number;
  lowStockCount: number;
  topProducts: { productId: string; productName: string; sku: string; quantitySold: number; revenue: number }[];
  paymentBreakdown: Record<string, number>;
}

export interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  profileImageUrl?: string;
  status: string;
  branchRoles: BranchRoleGrant[];
  createdAt: string;
}

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

export interface InviteCode {
  shopId: string;
  code: string;
  isActive: boolean;
  createdAt: string;
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