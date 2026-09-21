export const meta = {
  name: 'smartshop-page-gaps',
  description: 'Add the missing write actions to 9 existing SmartShop pages, one agent per file',
  phases: [{ title: 'Enhance pages', detail: 'one agent per existing page file' }],
}

const ROOT = 'C:/Users/diwak/OneDrive/Documents/Work Space/smartshop/smartshop-frontend'

const HOUSE_RULES = `
You are adding missing functionality to ONE existing React page in the SmartShop frontend.

REPO: ${ROOT}

=== ABSOLUTE FILE OWNERSHIP RULE ===
You may modify exactly ONE file: the one named in your task. Read it first, then use Edit
(or Write only if you are rewriting it wholesale) on that path and no other.
FORBIDDEN to create or modify: src/types/index.ts, src/lib/api.ts, src/App.tsx,
src/lib/routeRoles.ts, src/lib/routeRoles.test.ts, src/components/layout/AppLayout.tsx,
any other src/pages/*.tsx, anything under src/components/. Other agents own those, and the
routing/nav wiring is already done centrally. If you need a helper, inline it in your own file.

=== PRESERVE WHAT WORKS ===
This is an ADDITIVE change to a working page. Keep the existing queries, mutations, columns,
dialogs and styling intact unless your task explicitly says to change them. Do not "tidy"
unrelated code, do not rename existing state, do not reorder existing columns.

=== TOOLCHAIN FACTS (getting these wrong = build failure) ===
- React 18.3 + TypeScript strict. "noUnusedLocals" and "noUnusedParameters" are ON: an unused
  import or local is a HARD BUILD ERROR. Remove imports you stop using; prefix intentionally
  unused params with _ (the existing \`onChange={(_, p) => ...}\` idiom is correct).
- jsx: "react-jsx" — never \`import React from 'react'\`. Import hooks by name.
- MUI v6.3.1. DO NOT introduce \`<Grid>\`. For new layout use <Box sx={{ display: 'flex' }}> or
  <Box sx={{ display: 'grid', gridTemplateColumns: {...} }}>. (If the file ALREADY uses Grid,
  leave those parts alone — just don't add more.)
- MUI v6 date fields use \`slotProps={{ inputLabel: { shrink: true } }}\`, not \`InputLabelProps\`.
- TanStack Query v5: useQuery({queryKey, queryFn, enabled}), useMutation({mutationFn, onSuccess,
  onError}). Mutations expose \`isPending\` (NOT isLoading).

=== BACKEND ENVELOPE ===
Bodies are wrapped: { success, message, data, errors, timestamp } — so you read \`res.data.data\`.
The ONLY exception is /taxes/**, which returns RAW DTOs (read \`res.data\`); helpers for those
already exist in src/lib/api.ts (listTaxes, getTax, createTax, updateTax, addTaxRate,
listTaxRates, deleteTax) — import and use them rather than calling /taxes directly.

=== NULL HANDLING (critical) ===
\`spring.jackson.default-property-inclusion=non_null\` means a null field is an ABSENT KEY, never
the JSON value null. So never write \`x === null\`; render \`{x ?? '-'}\`, guard with \`if (!x)\`,
and prefill form inputs with \`x ?? ''\`.

=== THE FULL-REPLACE PUT RULE (the single most important thing in this workflow) ===
For products, customers, suppliers, expenses and categories, PUT reuses the exact same request
DTO as POST, and the service's \`applyRequest()\` assigns every scalar with NO null check.
That makes PUT a FULL REPLACE, not a PATCH: any field you omit is WIPED on the server.
Therefore every Edit dialog MUST:
  1. Prefill EVERY field from the row being edited (\`?? ''\` for absent keys).
  2. Resubmit EVERY field on save, not just the changed ones.
  3. Send \`field.trim() || undefined\` for genuinely-optional text, so that a box the user
     deliberately emptied is CLEARED server-side (absent key -> null -> assigned as null).
  4. Re-seed the form from the PUT response (\`res.data.data\`) — money is rounded to 2dp
     HALF_UP and text is trimmed server-side, so the response is authoritative.
Add a short comment saying PUT is a full replace, and an <Alert severity="info"> inside the edit
dialog telling the user that clearing a field clears it on the server.

=== ROLE GATING — READ THIS CAREFULLY ===
The backend has NO RoleHierarchy bean. So \`@PreAuthorize("hasRole('SHOP_ADMIN')")\` genuinely
DENIES a SUPER_ADMIN (403). But the frontend's \`hasRole()\` returns true for EVERY query once the
user holds SUPER_ADMIN. So \`<Can roles={[ROLES.SHOP_ADMIN]}>\` wrongly shows the control to
super-admins, who then get a 403.
\`Can\` accepts \`notRoles\`, which is how you express the exclusion:
    <Can roles={[ROLES.SHOP_ADMIN]} notRoles={[ROLES.SUPER_ADMIN]}> ... </Can>
Use exactly the role list given in your task. When the task says a role list EXCLUDES
SUPER_ADMIN, you MUST add \`notRoles={[ROLES.SUPER_ADMIN]}\`.
Import: \`import Can from '../components/guards/Can';\` and \`import { ROLES } from '../lib/routeRoles';\`
(\`useCan\` is also exported from that module if you need the boolean rather than a wrapper.)

=== HOUSE STYLE (already established in these files) ===
- \`const shopId = defaultShopId();\` from '../stores/shopStore'; queries \`enabled: Boolean(shopId)\`.
- \`import api, { extractErrorMessage } from '../lib/api';\`
- Errors: \`const [error, setError] = useState('')\` + \`<Alert severity="error" sx={{ mb: 2 }}
  onClose={() => setError('')}>\`; every mutation's onError does \`setError(extractErrorMessage(err))\`.
  The backend returns genuinely useful 400/409 text — surface it verbatim, never swallow it.
- \`<Table size="small">\`; empty state is a single row whose \`colSpan\` equals the REAL column
  count (if you add a column, update colSpan).
- \`<Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />\`
- \`<Dialog fullWidth maxWidth="sm">\` with \`<TextField margin="dense">\`.
- Money rendered with \`.toFixed(2)\`.
- Disable submit while \`mutation.isPending\` and when required fields are blank.
- Invalidate the right queryKeys in onSuccess.
- When a dialog is open, an error Alert placed on the page behind it is invisible — put the
  Alert INSIDE the DialogContent (or render it in both places) so dialog errors are seen.

=== SELF-VERIFY BEFORE YOU FINISH (required) ===
From ${ROOT} run exactly:
  npx tsc --noEmit -p tsconfig.json 2>&1 | grep -F "<YOUR FILE NAME>"
Read-only and safe to run concurrently. Fix ONLY errors whose path contains your own file;
errors in other files belong to other agents — do not touch them. Repeat until yours is clean.

Return the JSON summary per the schema. Be honest about anything you could not finish.
`

const TASKS = [
  {
    file: 'src/pages/ProductsPage.tsx',
    label: 'gap:ProductsPage',
    spec: `
Add an EDIT action to the products table, and fix the write-role gating.

NEW ENDPOINT: PUT /products/{id} body ProductRequest -> ApiResponse<Product>.

ROLE GATING FIX (applies to the EXISTING create and deactivate buttons too):
ProductController's POST, PUT and DELETE are all \`@PreAuthorize("hasRole('SHOP_ADMIN')")\`, and
there is no role hierarchy — a SUPER_ADMIN gets 403 on all three. The page currently shows the
create/deactivate controls to super-admins, which is a live bug. Wrap ALL THREE write controls
(New Product, Edit, Deactivate) in:
    <Can roles={[ROLES.SHOP_ADMIN]} notRoles={[ROLES.SUPER_ADMIN]}>
and add a comment explaining why the notRoles is required.

FULL-REPLACE DETAILS SPECIFIC TO PRODUCTS (see the house rule above):
- \`ProductRequest\` fields: shopId, categoryId, name, sku, barcode, brand, unit, description,
  purchasePrice, sellingPrice, vatApplicable, vatRate, taxId, reorderLevel, imageUrl, status.
- RELATIONS ARE CLEARED WHEN OMITTED: leaving out \`categoryId\` unassigns the category, and
  leaving out \`taxId\` unassigns the tax. So the edit dialog needs real pickers for both,
  prefilled from the row, with an explicit "— none —" option that sends undefined.
- Fields where null means "keep/default" rather than "wipe": \`status\` (unchanged),
  \`unit\` (defaults to "PCS"), \`vatApplicable\` (defaults true), \`vatRate\` (defaults to the
  shop's configured rate), \`reorderLevel\` (defaults 0). Since you prefill from the row you will
  be sending real values anyway — just don't send '' for numbers.
- \`shopId\` is inspected by ProductService: sending a different one errors with
  "Product cannot be moved to another shop". Always send the row's own shopId.
- 409 on duplicate SKU: "SKU X already exists in this shop".
- Prices are rounded 2dp HALF_UP server-side, so re-seed the form from the response.

DROPDOWN DATA the edit (and ideally the create) dialog needs:
- Categories: \`GET /categories?shopId=<uuid>\` -> ApiResponse<PageResponse<Category>> (send
  page=0, size=200). If that shape fails, fall back to \`GET /categories/tree\` ->
  ApiResponse<Category[]> and flatten it recursively. Verify which by reading
  smartshop-backend/src/main/java/com/smartshop/features/category/controller/CategoryController.java
  — do NOT guess.
- Taxes: use the existing helper \`listTaxes(shopId)\` imported from '../lib/api' (it already
  handles the /taxes raw-DTO envelope exception). Guard with \`enabled: Boolean(shopId)\`.

ALSO SURFACE \`warnings\`: \`ProductResponse.warnings\` is a string array whose only emitted value
is "Selling price is below purchase price". Show a small warning indicator on any row that has a
non-empty \`warnings\` array (e.g. a <Tooltip> wrapping a <WarningAmberIcon color="warning"
fontSize="small" />), and repeat the messages in the edit dialog. This is real backend signal the
UI currently throws away.

Reuse the existing create dialog for editing rather than building a second one: introduce
\`const [editing, setEditing] = useState<Product | null>(null)\`, switch the DialogTitle and the
submit handler on it, and extend the existing flat \`form\` state with any missing fields
(description, taxId, reorderLevel, imageUrl, status) so both paths share it.
`,
  },
  {
    file: 'src/pages/CustomersPage.tsx',
    label: 'gap:CustomersPage',
    spec: `
Add an EDIT action to the customers table.

NEW ENDPOINT: PUT /customers/{id} body CustomerRequest -> ApiResponse<Customer>.
Roles: hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER') — SUPER_ADMIN IS included
here, so a plain <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER]}>
is correct and you must NOT add notRoles.

\`CustomerRequest\` = { shopId, name, phone, email, address }. PUT is a FULL REPLACE per the house
rule: prefill and resubmit all of name/phone/email/address, sending \`trim() || undefined\` for the
three optional ones so clearing a box clears the server value.

The file already has \`const FIELDS = ['name','phone','email','address'] as const;\` and a
\`form: Record<string, string>\` — reuse both for the edit path instead of adding a parallel
structure. Introduce \`editing: Customer | null\`, prefill with
\`Object.fromEntries(FIELDS.map((f) => [f, customer[f] ?? '']))\`, and switch the dialog title
and submit target on it. Keep the existing create and delete behaviour unchanged.

Note \`loyaltyPoints\` is NOT part of CustomerRequest — it is server-managed. Do not put it in the
form, and do not send it.

Re-seed the form from the PUT response and invalidate ['customers'].
`,
  },
  {
    file: 'src/pages/SuppliersPage.tsx',
    label: 'gap:SuppliersPage',
    spec: `
Add an EDIT action to the suppliers table.

NEW ENDPOINT: PUT /suppliers/{id} body SupplierRequest -> ApiResponse<Supplier>.
Roles: hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF') — SUPER_ADMIN IS
included, so no notRoles.

\`SupplierRequest\` = { shopId, name, companyName, phone, email, address, panNumber, status }.
PUT is a FULL REPLACE per the house rule — prefill and resubmit everything, with
\`trim() || undefined\` for the optional text fields.
\`status\` is the one field where null means "leave unchanged"; still, since you prefill from the
row, expose it as a select over Status = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED' shown only when
editing, and send the selected value.

The file already has \`const FIELDS = ['name','companyName','phone','email','address','panNumber']
as const;\` with dialog labels derived via \`f.replace(/([A-Z])/g, ' $1')\` — reuse that machinery
for the edit dialog rather than hand-writing six more TextFields. Introduce
\`editing: Supplier | null\`. Keep the existing create and deactivate behaviour unchanged
(deactivate is a soft delete — leave its label as is).

Re-seed from the PUT response and invalidate ['suppliers'].
`,
  },
  {
    file: 'src/pages/ExpensesPage.tsx',
    label: 'gap:ExpensesPage',
    spec: `
Add an EDIT action to the expenses table.

NEW ENDPOINT: PUT /expenses/{id} body ExpenseRequest -> ApiResponse<Expense>.
Roles: hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER') — SUPER_ADMIN IS included, no notRoles.

\`ExpenseRequest\` = { shopId, branchId, title, category, amount, expenseDate, paymentMethod, note }.
PUT is a FULL REPLACE per the house rule. Specifics:
- \`branchId\` is CLEARED when omitted, so the edit dialog needs a real branch picker prefilled
  from the row (options from \`useAuthStore\` \`user.branchRoles\` entries that have a branchId,
  plus an explicit "— none —" that sends undefined). This is a relation, not a scalar.
- \`expenseDate\` is the one field where null means "keep the existing date". Expose it as a
  <TextField type="date" slotProps={{ inputLabel: { shrink: true } }} /> prefilled from the row
  and always send it.
- \`amount\` is \`@DecimalMin("0.01")\` — a 0 or blank amount is a 400. Block submit unless
  \`Number(form.amount) >= 0.01\`, and rely on the server message for anything else. It is rounded
  2dp HALF_UP server-side, so re-seed from the response.
- The existing payment-method dropdown is missing the \`OTHER\` constant. Fix that by mapping the
  exported \`PAYMENT_METHODS\` array from '../types' instead of a hand-written list, so create and
  edit both offer all six.

Also ADD an \`expenseDate\` column to the table if it is not already displayed (the field is
always present on the response and is the main thing users sort expenses by mentally) — and if
you add a column, update the empty-state \`colSpan\` to match.

Introduce \`editing: Expense | null\`, reuse the existing \`form\` state shape, switch the dialog
title and submit target on it, and keep create/delete unchanged. Invalidate ['expenses'].
`,
  },
  {
    file: 'src/pages/CategoriesPage.tsx',
    label: 'gap:CategoriesPage',
    spec: `
Add EDIT and DELETE actions to the category tree. This page is currently create-only.

NEW ENDPOINTS:
- PUT /categories/{id} body CategoryRequest -> ApiResponse<Category>
- DELETE /categories/{id} -> ApiResponse<...>
Both, plus the existing POST, are \`@PreAuthorize("hasRole('SHOP_ADMIN')")\` with NO role
hierarchy, so a SUPER_ADMIN gets 403 on all three. Gate every write control with:
    <Can roles={[ROLES.SHOP_ADMIN]} notRoles={[ROLES.SUPER_ADMIN]}>
and comment why. That includes the EXISTING create button, which is currently shown to
super-admins and 403s — fix it.

YOU MUST VERIFY THE DELETE SEMANTICS BEFORE WRITING THE UI. Read
smartshop-backend/src/main/java/com/smartshop/features/category/service/CategoryService.java
and check whether \`delete\` calls \`categoryRepository.delete(...)\` (HARD delete) or sets
\`status\` to INACTIVE (SOFT delete). Label the button and the window.confirm text to match what
you actually find — "Delete permanently" vs "Deactivate" — and say which it is in your notes.
Also check whether it refuses to delete a category that still has children or products, and
surface that server message.

\`CategoryRequest\` = { shopId, parentId, name, code, description, status }. PUT is a FULL REPLACE
per the house rule, with two category-specific traps:
- Omitting \`parentId\` PROMOTES the category to a root node. So the edit dialog needs a parent
  picker prefilled from the row, including an explicit "— none (top level) —" option. Exclude the
  category being edited (and, ideally, its own descendants) from its own parent options so a user
  cannot create a cycle.
- \`CategoryResponse.children\` is hardcoded null on the PUT response — only \`GET /categories/tree\`
  populates it. So do NOT try to merge the PUT response into the tree; just invalidate the tree
  query (\`queryClient.invalidateQueries({ queryKey: ['categories'] })\` — match the existing key).
- \`status\` null means unchanged; expose it as a select over 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED'
  when editing.
- 409 on duplicate code: "Category code X already exists in this shop".

The page renders a recursive \`CategoryNode({ category, depth })\` with a \`Collapse\`. Add the Edit
and Delete <IconButton>s into that node's row, right-aligned, and make sure clicking them does
NOT toggle the collapse (call \`e.stopPropagation()\` on the button handlers). The node component
will need the callbacks passed down — extend its props rather than reaching for context.

Because the tree is nested, build the parent-picker options by flattening the tree recursively
into a list of \`{ id, label }\` where label is indented by depth (e.g. \`'—'.repeat(depth) + name\`).
`,
  },
  {
    file: 'src/pages/SalesPage.tsx',
    label: 'gap:SalesPage',
    spec: `
The sales page is currently read-only (list + a detail dialog with tax totals). Add payment
management to the existing detail dialog.

NEW ENDPOINT 1 — update the document-level payment status:
PUT /sales/{id}/payment-status  body { paymentStatus } -> ApiResponse<Sale>
  * The body is JSON. \`?paymentStatus=PAID\` as a query param is a 400 — it must be the body.
    The type \`PaymentStatusRequest\` is exported from '../types'.
  * Only three values: 'UNPAID' | 'PARTIAL' | 'PAID' (exported as PAYMENT_STATUSES).
  * Roles: hasAnyRole('SHOP_ADMIN','ACCOUNTANT') — this EXCLUDES SUPER_ADMIN, MANAGER and
    CASHIER. So gate with
        <Can roles={[ROLES.SHOP_ADMIN, ROLES.ACCOUNTANT]} notRoles={[ROLES.SUPER_ADMIN]}>
    and comment that the exclusion is required because hasRole() grants SUPER_ADMIN everything.
  * There is NO transition validation — any status can go to any other. And it does NOT recompute
    from the payment ledger, so it can disagree with the recorded payments. Add helper text
    saying it is a manual override.

NEW ENDPOINT 2 — the payment ledger for a sale:
GET  /payments?saleId=<uuid> -> ApiResponse<Payment[]>   (unpaged plain array; saleId REQUIRED)
     Roles: SUPER_ADMIN, SHOP_ADMIN, MANAGER, CASHIER, ACCOUNTANT.
POST /payments body PaymentRequest -> 201 ApiResponse<Payment>
     Roles: SUPER_ADMIN, SHOP_ADMIN, MANAGER, CASHIER — ACCOUNTANT can READ but NOT POST.
     So the "Record payment" button gate is
         <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER]}>
     (no notRoles — SUPER_ADMIN is genuinely allowed here).

TYPES (already exported from '../types'): Payment, PaymentRequest, PaymentStatusRequest,
PaymentMethod, PAYMENT_METHODS, PaymentStatus, PAYMENT_STATUSES, PaymentState.

THE BIGGEST TYPE TRAP: \`Payment.paymentStatus\` is a \`PaymentState\`
('PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED') — a per-transaction state. It is NOT the
document-level \`PaymentStatus\` ('UNPAID' | 'PARTIAL' | 'PAID') on the Sale. They are different
enums with different values. Do not mix them, and do not feed one into the other's Chip colour
map. Add a comment.

OTHER BEHAVIOUR TO ENCODE:
- \`PaymentRequest\` = { saleId, amount, paymentMethod, referenceNumber }.
- Non-CASH payments are routed through MockPaymentGateway, which may OVERWRITE the
  \`referenceNumber\` you sent with \`MOCK-<nanotime>\`. So do not assume the response echoes your
  reference; render whatever comes back, and put helper text on the field saying the gateway may
  replace it for non-cash methods.
- Recording a payment MUTATES the sale's own \`paymentStatus\`. After a successful POST you must
  invalidate BOTH the payments query and the sales list/detail, and refresh the open dialog's
  sale — otherwise the header still shows the stale status.
- Show a computed "Paid so far" (sum of COMPLETED payments only — do not count FAILED or PENDING)
  and "Balance" (\`sale.totalAmount - paidCompleted\`) in the dialog. Default the new-payment
  amount input to that balance when it is positive.

UI: inside the existing detail dialog, below the current items/tax section, add a "Payments"
section — a <Divider>, a small table (When / Amount / Method / Status Chip / Reference /
Received by) with an informative empty state ("No payments recorded against this invoice yet."),
the Paid/Balance summary, a "Record payment" button opening a small nested Dialog (amount,
method select over PAYMENT_METHODS, reference), and the payment-status override control.
Gate the payments query on \`enabled: Boolean(selected?.id)\` so it only runs when the dialog is
open. Keep the existing tax-totals rendering and \`groupTaxByRate\` usage exactly as it is.
`,
  },
  {
    file: 'src/pages/PurchasesPage.tsx',
    label: 'gap:PurchasesPage',
    spec: `
The purchases page currently lists purchases and creates them (with line items and tax
breakdown). Add a DETAIL dialog with payment management.

NEW ENDPOINT 1 — fetch one purchase (the list rows do NOT carry \`items\`):
GET /purchases/{id} -> ApiResponse<Purchase>, with \`items: PurchaseItem[]\` populated.
Use \`useQuery({ queryKey: ['purchase', selectedId], enabled: Boolean(selectedId) })\`.

NEW ENDPOINT 2 — document-level payment status:
PUT /purchases/{id}/payment-status body { paymentStatus } -> ApiResponse<Purchase>
  * JSON body, not a query param (\`PaymentStatusRequest\` from '../types'). Three values only.
  * Roles: hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','ACCOUNTANT') — SUPER_ADMIN IS included here
    (unlike the sales equivalent), so NO notRoles.
  * No transition validation, and it does not recompute from the payment ledger — label it a
    manual override.

NEW ENDPOINT 3 — the purchase payment ledger:
GET  /purchases/{purchaseId}/payments -> ApiResponse<PurchasePayment[]>  (unpaged array)
POST /purchases/{purchaseId}/payments body PurchasePaymentRequest -> 201 ApiResponse<PurchasePayment>
  * BOTH are \`hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER')\` ONLY — narrower than the sales
    side; ACCOUNTANT and CASHIER get 403 on both, including the READ. So gate the whole payments
    section (not just the button) with
        <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER]}>
    and skip the query when the user is not permitted, so an accountant viewing a purchase does
    not trigger a pointless 403.
  * They additionally run \`ShopAdminGuard.requireShopAdmin\`, so a user outside the purchase's own
    shop is rejected even with the right role — surface the server message.

TYPES (already exported from '../types'): Purchase, PurchaseItem, PurchasePayment,
PurchasePaymentRequest, PaymentStatusRequest, PAYMENT_METHODS, PAYMENT_STATUSES.

SERVER VALIDATION TO SURFACE VERBATIM (these are good messages — do not pre-empt them with
guesses, just display them):
  400 "Purchase is already fully paid"
  400 "Payment amount (<x>) exceeds the remaining balance (<y>)"
\`PurchasePaymentRequest.paymentDate\` is \`@PastOrPresent\`, so set \`max\` on the date input to
today and default it to today. \`PurchasePaymentRequest\` =
{ paymentDate, amount, paymentMethod, referenceNumber, notes }.

Recording a payment MUTATES the purchase's \`paymentStatus\` — after a successful POST invalidate
the payments query, the ['purchase', id] detail query AND the ['purchases'] list.

UI: make each list row clickable (hover background + cursor pointer, matching how SalesPage does
it) to open a detail Dialog (fullWidth maxWidth="md") showing: header facts (purchaseNumber,
purchaseDate, supplierName, branchName, createdByName, paymentStatus Chip); the line items table
(Item with productName + sku caption / Qty / Unit Cost / Discount / VAT / Line Total); the
existing tax-total presentation if you can reuse \`computeTaxBreakdown\`/\`TaxTotals\` from
'../lib/tax' the way the create form already does — note purchase items use \`unitCost\`, NOT
\`unitPrice\`; then a "Payments" section with the ledger table (Date / Amount / Method / Reference
/ Notes / Recorded by), a Paid-so-far and Balance summary (\`totalAmount - sum(amount)\`), a
"Record payment" nested Dialog defaulting the amount to the outstanding balance, and the
payment-status override control.
Leave the existing create dialog, \`interface Line\`, and tax-breakdown logic untouched.
`,
  },
  {
    file: 'src/pages/UsersPage.tsx',
    label: 'gap:UsersPage',
    spec: `
Improve the users page: add status management, widen who can use it, and replace the ad-hoc
local row type with the real shared one.

CHANGE 1 — use the shared type. The file declares a local
\`interface UserRow { id, firstName, lastName, email, status, roles: string[] }\` that does not
match the wire. \`UserResponse\` is now exported from '../types' as \`User\`:
  { id, firstName, lastName, fullName, email, phone?, profileImageUrl?, status: UserStatus,
    branchRoles: UserBranchRoleRef[], createdAt }
Delete the local interface and use \`User\`. NOTE: there is no \`roles: string[]\` field on the wire
— role information comes as \`branchRoles\`, an array of \`UserBranchRoleRef\`
({ branchId?, branchName?, shopId?, role }) where \`branchId\`/\`branchName\`/\`shopId\` are ABSENT for
a shop-wide grant. So wherever the page currently renders \`roles\`, derive the distinct role names
instead: \`[...new Set(u.branchRoles.map((b) => b.role))]\`. Guard every optional read.

CHANGE 2 — widen access. The query is currently gated on
\`enabled: Boolean(user?.roles.includes('SUPER_ADMIN'))\`, but \`GET /users\` is
\`hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')\`. Change the gate so SHOP_ADMINs can use the page too
(use \`useCan()\` or \`hasRole\` from the auth store with both roles). Note \`user.roles\` on the auth
store IS the right place to read the caller's own top-level roles — that part is fine.

CHANGE 3 — add search and pagination. \`GET /users?page&size&sort&search\` ->
ApiResponse<PageResponse<User>>. \`sort\` defaults server-side to "createdAt,desc" — do NOT send a
sort param. Add a search TextField (omit the param entirely when blank), reset page to 0 on
search change, and include page+search in the queryKey. Add the standard <Pagination> footer.

CHANGE 4 — the actual gap: change a user's status.
PATCH /users/{id}/status body { status } -> ApiResponse<User> "User status updated"
  * \`UserStatusRequest\` is exported from '../types'. \`UserStatus\` = 'ACTIVE' | 'BLOCKED' | 'SUSPENDED'.
  * Roles: \`hasRole('SUPER_ADMIN')\` ONLY — a SHOP_ADMIN gets 403. Since SUPER_ADMIN is the
    allowed role here, a plain <Can roles={[ROLES.SUPER_ADMIN]}> is correct (no notRoles).
    So: SHOP_ADMINs see the list but not the status control.
  * UI: a status <Chip> per row (ACTIVE 'success', BLOCKED 'error', SUSPENDED 'warning') plus, for
    super-admins, a small action to change it — either a select in a confirm Dialog or a
    row <IconButton> opening one. Include a window.confirm-style warning for BLOCKED, since it
    locks the user out. Invalidate ['users'] on success.
  * Do not offer the control on the caller's OWN row (compare against the auth store's userId) —
    blocking yourself is a foot-gun.

Also add columns for \`fullName\` (prefer it over concatenating firstName/lastName — it is derived
server-side), \`phone ?? '-'\`, the derived roles, status, and \`createdAt\`. Update the empty-state
\`colSpan\` to the new column count. Keep the existing create-user flow that posts to
\`/auth/register\` exactly as it is, and keep the existing <Can roles={[ROLES.SUPER_ADMIN]}> wrapper
around it.
`,
  },
  {
    file: 'src/pages/TaxSettingsPage.tsx',
    label: 'gap:TaxSettingsPage',
    spec: `
This is already the most complete page in the app (react-hook-form + zod dialogs for create tax,
edit tax and add rate, plus an isActive toggle). Two endpoints remain unwired. Make a SURGICAL
addition — do not restructure the existing dialogs or validation.

GAP 1 — delete a tax:
Use the helper \`deleteTax(id)\` already exported from '../lib/api' (DELETE /taxes/{id}).
  * THIS IS A HARD DELETE. \`TaxService.delete\` calls \`taxRepository.delete(tax)\`; it does NOT
    soft-deactivate. The row and its rate history are gone permanently.
  * Because of that, the UI must steer users to the existing isActive toggle instead. Requirements:
      - Label the action "Delete permanently" (never just "Delete").
      - Use a confirmation Dialog, NOT a bare window.confirm, and in it: state that this is
        permanent and removes the rate history, and point out that deactivating the tax is the
        reversible alternative for a tax that has been used on real documents.
      - Require the user to type the tax name to enable the confirm button (a type-to-confirm
        guard). This is genuinely destructive and irreversible.
      - Surface any server error verbatim — a tax referenced by products may fail on a foreign-key
        constraint, and that message is the only signal the user gets.
      - Gate it with <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]}> to match the page's
        route access, and note that TaxService additionally runs \`ShopAdminGuard.requireShopAdmin\`.
      - Invalidate the same queryKey the page already uses for the tax list.

GAP 2 — the rate history endpoint:
Use the helper \`listTaxRates(taxId)\` already exported from '../lib/api'
(GET /taxes/{id}/rates, returns a RAW \`TaxRate[]\` — the /taxes controller does not use the
ApiResponse envelope; the helper handles that).
  * Add a "Rate history" action per tax that opens a Dialog listing every rate: Rate, Valid from,
    Valid to, using the page's EXISTING \`formatRateWindow\` helper so the formatting matches.
  * Sort newest-first by \`validFrom\` descending, and mark the currently-effective rate (the one
    whose window contains today, using the page's existing \`today()\` helper) with a Chip
    e.g. "CURRENT".
  * \`validTo\` is nullable/absent for an open-ended rate — render '—' or "ongoing", never the
    string "null".
  * Gate on \`enabled: Boolean(ratesTarget)\` so it only fetches when the dialog is open, and give
    it its own queryKey like ['tax-rates', taxId]. Invalidate that key after the existing
    add-rate mutation succeeds, so the history reflects a newly added rate.

Reuse the file's established idioms — its existing Dialog structure, \`formatRateWindow\`,
\`today()\`, the MUI v6 \`slotProps={{ inputLabel: { shrink: true } }}\` pattern, and its existing
error-Alert handling. Do not convert anything to a different form library or change the zod schemas.
`,
  },
]

const SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['file', 'endpointsWired', 'typecheckClean', 'notes'],
  properties: {
    file: { type: 'string' },
    endpointsWired: {
      type: 'array',
      description: 'backend endpoints this page now calls that it did not before, as "METHOD /path"',
      items: { type: 'string' },
    },
    preservedExisting: {
      type: 'boolean',
      description: 'true if all pre-existing behaviour on the page still works',
    },
    typecheckClean: {
      type: 'boolean',
      description: 'true only if tsc reports ZERO errors whose path contains your file',
    },
    remainingErrors: { type: 'array', items: { type: 'string' } },
    backendFactsVerified: {
      type: 'array',
      description: 'anything you confirmed by reading backend source (e.g. soft vs hard delete)',
      items: { type: 'string' },
    },
    notes: { type: 'array', items: { type: 'string' } },
  },
}

phase('Enhance pages')

const results = await parallel(
  TASKS.map((t) => () =>
    agent(
      HOUSE_RULES +
        '\n\n=== YOUR ASSIGNED FILE: ' +
        t.file +
        ' ===\n' +
        t.spec +
        '\n\nBefore editing: Read your file in full, and Read src/types/index.ts for the exact ' +
        'type shapes. Read src/components/guards/Can.tsx to confirm the Can/useCan API. ' +
        'Then make the change with Edit, then self-verify with tsc as instructed. ' +
        'Remember: you own ' + t.file + ' and nothing else.',
      { label: t.label, phase: 'Enhance pages', schema: SCHEMA },
    ),
  ),
)

const done = results.filter(Boolean)
log(`${done.length}/${TASKS.length} enhancement agents returned`)

return {
  pages: done,
  failed: TASKS.filter((t, i) => !results[i]).map((t) => t.file),
}
