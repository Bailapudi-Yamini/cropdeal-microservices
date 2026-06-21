export interface User {
  id: number;
  name: string;
  email: string;
  role: 'FARMER' | 'DEALER' | 'ADMIN';
  phone?: string;
  location?: string;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  name: string;
  email: string;
  role: 'FARMER' | 'DEALER' | 'ADMIN';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: 'FARMER' | 'DEALER';
  phone?: string;
  location?: string;
}

export interface CropListing {
  id: number;
  farmerId: number;
  farmerName: string;
  cropName: string;
  cropType: string;
  quantityAvailable: number;
  unit: string;
  pricePerUnit: number;
  location: string;
  description?: string;
  status: 'AVAILABLE' | 'SOLD' | 'INACTIVE';
  createdAt: string;
}

export interface CropListingRequest {
  cropName: string;
  cropType: string;
  quantityAvailable: number;
  unit: string;
  pricePerUnit: number;
  location: string;
  description?: string;
}

export interface Order {
  id: number;
  cropListingId: number;
  cropName: string;
  farmerId: number;
  farmerName: string;
  dealerId: number;
  dealerName: string;
  quantity: number;
  pricePerUnit: number;
  totalAmount: number;
  status: 'PENDING' | 'NEGOTIATING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
}

export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  status: 'INITIATED' | 'SUCCESS' | 'FAILED' | 'REFUNDED';
  transactionId?: string;
  razorpayOrderId?: string;
  paymentGatewayRef?: string;
  createdAt: string;
  paidAt?: string;
}

export interface Negotiation {
  id: number;
  orderId: number;
  proposedBy: 'DEALER' | 'FARMER';
  proposedByName: string;
  proposedPrice: number;
  message?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'COUNTERED';
  createdAt: string;
}

export type NotificationType =
  | 'CROP_POSTED'
  | 'ORDER_PLACED'
  | 'NEGOTIATION_UPDATE'
  | 'PAYMENT_SUCCESS'
  | 'PAYMENT_FAILED';

export interface AppNotification {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  referenceId?: number;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: AppNotification[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
  unreadCount: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
