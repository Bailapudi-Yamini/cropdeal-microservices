import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import { ApiResponse, CropListing, CropListingRequest, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class CropService {
  private base = `${environment.apiUrl}/crops`;

  constructor(private http: HttpClient) {}

  getAvailable(page = 0, size = 10, cropType?: string, location?: string): Observable<PagedResponse<CropListing>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (cropType) params = params.set('cropType', cropType);
    if (location) params = params.set('location', location);
    return this.http.get<ApiResponse<PagedResponse<CropListing>>>(this.base, { params }).pipe(map(r => r.data));
  }

  search(keyword: string, page = 0, size = 10): Observable<PagedResponse<CropListing>> {
    const params = new HttpParams().set('keyword', keyword).set('page', page).set('size', size);
    return this.http.get<ApiResponse<PagedResponse<CropListing>>>(`${this.base}/search`, { params }).pipe(map(r => r.data));
  }

  getMyListings(): Observable<CropListing[]> {
    return this.http.get<ApiResponse<CropListing[]>>(`${this.base}/my`).pipe(map(r => r.data));
  }

  getById(id: number): Observable<CropListing> {
    return this.http.get<ApiResponse<CropListing>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  create(req: CropListingRequest): Observable<CropListing> {
    return this.http.post<ApiResponse<CropListing>>(this.base, req).pipe(map(r => r.data));
  }

  update(id: number, req: Partial<CropListingRequest>): Observable<CropListing> {
    return this.http.put<ApiResponse<CropListing>>(`${this.base}/${id}`, req).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
