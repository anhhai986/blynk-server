export function ProductMetadataFieldAdd(data = {}) {
  return {
    type: 'PRODUCT_METADATA_FIELD_ADD',
    data: data
  };
}

export function ProductMetadataFieldDelete(data = {}) {
  return {
    type: 'PRODUCT_METADATA_FIELD_DELETE',
    data: data
  };
}

export function ProductMetadataFieldValuesUpdate(data = {}) {
  return {
    type: 'PRODUCT_METADATA_FIELD_VALUES_UPDATE',
    data: data
  };
}

export function ProductMetadataFieldInvalidFlagUpdate(data = {}) {
  return {
    type: 'PRODUCT_METADATA_FIELD_INVALID_FLAG_UPDATE',
    data: data
  };
}

export function ProductMetadataFieldsOrderUpdate(data = {}) {
  return {
    type: 'PRODUCT_METADATA_FIELDS_ORDER_UPDATE',
    data: data
  };
}

export function ProductMetadataUpdateInvalidFlag(data = {}) {
  return {
    type: 'PRODUCT_METADATA_UPDATE_INVALID_FLAG',
    data: data
  };
}

export function ProductInfoUpdateValues(data = {}) {
  return {
    type: 'PRODUCT_INFO_UPDATE_VALUES',
    data: data
  };
}

export function ProductInfoUpdateInvalidFlag(data = false) {
  return {
    type: 'PRODUCT_INFO_UPDATE_INVALID_FLAG',
    data: data
  };
}
