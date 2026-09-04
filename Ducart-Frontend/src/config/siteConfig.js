const siteConfig = {
  brandName: "Ducart",
  ownerName: "Xintao He",

  addressShort: "Galway, Ireland",
  addressFull: "Galway, County Galway, Ireland",

  email: "xintaohe49@gmail.com",
  phone: "+353 0851714732",
  phoneLink: "+3530851714732",
  whatsapp: "3530851714732",

  github: "https://github.com/RedBone12",
  linkedin: "https://www.linkedin.com/in/xintao-he-789541383/",

  mapQuery: "Galway Ireland",

  currency: "EUR",
  locale: "en-IE",

  shippingFee: 4.99,
  freeShippingThreshold: 100,

   paymentModes: [
    { value: "COD", label: "Cash on Delivery" },
    { value: "Card Demo", label: "Credit/Debit Card (Demo)" },
    { value: "PayPal Demo", label: "PayPal (Demo)" },
    { value: "Bank Transfer", label: "Bank Transfer" },
  ],
};

export function formatCurrency(value) {
  return new Intl.NumberFormat(siteConfig.locale, {
    style: "currency",
    currency: siteConfig.currency,
  }).format(Number(value) || 0);
}

export default siteConfig;