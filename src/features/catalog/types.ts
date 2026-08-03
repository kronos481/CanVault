export type VerificationStatus =
  | 'unverified'
  | 'community_submitted'
  | 'source_verified'
  | 'manufacturer_verified'
  | 'deprecated'
  | 'disputed';

export interface CatalogBrand {
  id: string;
  slug: string;
  displayName: string;
  legalName: string | null;
  verificationStatus: VerificationStatus;
}

export interface CatalogCanLine {
  id: string;
  brandId: string;
  slug: string;
  displayName: string;
  defaultVolumeMl: number | null;
  pressureType: string | null;
  paintType: string | null;
  finish: string | null;
  verificationStatus: VerificationStatus;
}

export interface CatalogColor {
  id: string;
  canLineId: string;
  officialName: string;
  officialCode: string;
  hexApproximation: string | null;
  verificationStatus: VerificationStatus;
}
