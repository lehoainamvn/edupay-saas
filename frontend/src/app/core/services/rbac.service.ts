import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { FeatureFlag, MenuItem, OverrideFeatureRequest, UserNavigation } from '../models/rbac.models';

@Injectable({ providedIn: 'root' })
export class RbacService {
  private readonly http = inject(HttpClient);
  private readonly API = environment.apiBaseUrl;

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly roles = signal<string[]>([]);
  readonly permissions = signal<Set<string>>(new Set());
  readonly features = signal<Map<string, boolean>>(new Map());
  readonly menus = signal<MenuItem[]>([]);
  readonly isLoaded = signal<boolean>(false);
  readonly loading = signal<boolean>(false);

  readonly isOwner = computed(() => this.roles().includes('OWNER'));

  // ─── API CALLS ──────────────────────────────────────────────────────────────
  loadNavigation(): Observable<UserNavigation> {
    this.loading.set(true);
    return this.http.get<ApiResponse<UserNavigation>>(`${this.API}/me/navigation`).pipe(
      map((res) => {
        if (res && res.data) {
          return res.data;
        }
        throw new Error('Dữ liệu điều hướng không hợp lệ');
      }),
      tap((data) => {
        this.roles.set(data.roles || []);
        this.permissions.set(new Set(data.permissions || []));

        const featMap = new Map<string, boolean>();
        if (data.features) {
          Object.entries(data.features).forEach(([k, v]) => featMap.set(k, Boolean(v)));
        }
        this.features.set(featMap);

        this.menus.set(data.menus || []);
        this.isLoaded.set(true);
        this.loading.set(false);
      }),
      catchError((err) => {
        this.loading.set(false);
        this.isLoaded.set(false);
        return throwError(() => err);
      })
    );
  }

  getFeatures(): Observable<FeatureFlag[]> {
    return this.http.get<ApiResponse<FeatureFlag[]>>(`${this.API}/features`).pipe(
      map((res) => res.data || [])
    );
  }

  overrideFeature(featureId: string, request: OverrideFeatureRequest): Observable<FeatureFlag> {
    return this.http.put<ApiResponse<FeatureFlag>>(`${this.API}/features/${featureId}/override`, request).pipe(
      map((res) => res.data),
      tap((updated) => {
        if (updated) {
          this.features.update((m) => {
            const next = new Map(m);
            next.set(updated.featureId, updated.enabled);
            return next;
          });
        }
      })
    );
  }

  // ─── CHECK HELPERS ──────────────────────────────────────────────────────────
  hasPermission(permission: string | string[], mode: 'AND' | 'OR' = 'OR'): boolean {
    if (this.isOwner()) {
      return true; // Owner luôn có toàn quyền
    }

    const currentPerms = this.permissions();
    const required = Array.isArray(permission) ? permission : [permission];

    if (required.length === 0) return true;

    if (mode === 'AND') {
      return required.every((p) => currentPerms.has(p));
    } else {
      return required.some((p) => currentPerms.has(p));
    }
  }

  hasRole(role: string | string[]): boolean {
    const currentRoles = this.roles();
    const required = Array.isArray(role) ? role : [role];
    return required.some((r) => currentRoles.includes(r));
  }

  hasFeature(featureId: string): boolean {
    return this.features().get(featureId) ?? false;
  }

  clear(): void {
    this.roles.set([]);
    this.permissions.set(new Set());
    this.features.set(new Map());
    this.menus.set([]);
    this.isLoaded.set(false);
  }
}

