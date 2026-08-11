type AddressFieldsProps = {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  inputStyle: string;
  labelStyle: string;
};

export function AddressFields({
  street,
  city,
  state,
  zipCode,
  onChange,
  inputStyle,
  labelStyle,
}: AddressFieldsProps) {
  return (
    <section className="space-y-2">
      <div>
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-gray-700">Endereço</h3>
        <p className="mt-1 text-[11px] text-gray-500">
          Opcional. Se preencher um campo, informe rua, cidade, estado e CEP.
        </p>
      </div>

      <div className="flex flex-col">
        <label className={labelStyle}>Rua</label>
        <input
          name="street"
          placeholder="Rua, número e complemento"
          value={street}
          onChange={onChange}
          className={inputStyle}
        />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="flex flex-col">
          <label className={labelStyle}>Cidade</label>
          <input
            name="city"
            placeholder="Cidade"
            value={city}
            onChange={onChange}
            className={inputStyle}
          />
        </div>

        <div className="flex flex-col">
          <label className={labelStyle}>Estado</label>
          <input
            name="state"
            placeholder="UF"
            value={state}
            onChange={onChange}
            className={inputStyle}
            maxLength={2}
          />
        </div>
      </div>

      <div className="flex flex-col">
        <label className={labelStyle}>CEP</label>
        <input
          name="zipCode"
          placeholder="00000-000"
          value={zipCode}
          onChange={onChange}
          className={inputStyle}
        />
      </div>
    </section>
  );
}
