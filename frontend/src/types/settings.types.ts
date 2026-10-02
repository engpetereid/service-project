export interface SettingResponse {
  id: number;
  settingKey: string;
  settingValue: string;
}

export interface UpdateSettingRequest {
  settingKey: string;
  settingValue: string;
}

export interface PublicSettingsResponse {
  maxNoteScore: number;
}

export interface AcademicYearResponse {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  current: boolean;
}
