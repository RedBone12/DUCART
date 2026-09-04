import passwordValidator from "password-validator"
var schema = new passwordValidator();

schema
    .is().min(8)                                    // Minimum length 8
    .is().max(100)                                  // Maximum length 100
    .has().uppercase(1)                              // Must have 1 uppercase letter
    .has().lowercase(1)                              // Must have 1 lowercase letter
    .has().digits(1)                                // Must have 1 digit
    .has().not().spaces()                           // Should not have spaces
    .is().not().oneOf(['Passw0rd', 'Password123']); // Blacklist these values


export default function formValidators(e) {
    let { name } = e.target
    const value = e.target.value ?? ""
    switch (name) {
        case 'name':
        case 'username':
        case 'color':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (value.length < 3 || value.length > 50)
                return name + " Field Length must be within 3-50 characters"
            else
                return ""

        case 'email':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (value.length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))
                return "Invalid Email Address"
            else
                return ""

        case 'subject':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (value.length < 10 || value.length > 200)
                return name + " Field Length must be within 10-200 characters"
            else
                return ""

        case 'password':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (!schema.validate(value))
                return "Invalid Password! Its Length must be within 8-100 characters, Must Contains atleast 1 Upper Case Character,1 lower Case Character and 1 Digit and It Should not contains any space"
            else
                return ""

        case 'phone':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (!/^\+?[0-9]{7,15}$/.test(value))
                return "Invalid Phone Number"
            else
                return ""


        case 'size':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (value.length > 50)
                return name + " Field Length must be less than 50 characters"
            else
                return ""


        case 'basePrice':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (!Number.isFinite(Number(value)) || Number(value) <= 0)
                return "Price Must be a Value Greater than 0"
            else
                return ""

        case 'discount':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (!Number.isFinite(Number(value)) || Number(value) < 0 || Number(value) > 100)
                return "Discount Field Must Be 0-100"
            else
                return ""

        case 'stockQuantity':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (!Number.isInteger(Number(value)) || Number(value) < 0)
                return "Stock Quantity Must be a Non-negative Integer"
            else
                return ""

        case 'message':
            if (!String(value).trim())
                return name + " Field is Mandatory"
            else if (value.length < 50 || value.length > 2000)
                return name + " Field Length must be within 50-2000 characters"
            else
                return ""
        default:
            return ""
    }
}
